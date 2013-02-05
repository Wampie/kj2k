/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.jet.j2k;

import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.visitors.ClassVisitor;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * @author ignatov
 */
public class JavaToKotlinTranslator {
    private JavaToKotlinTranslator() {
    }

    private static final Converter CONVERTER = new Converter();

    @Nullable
    private static PsiFile createFile(@NotNull String text) {
        JavaCoreProjectEnvironment javaCoreEnvironment = setUpJavaCoreEnvironment();
        return PsiFileFactory.getInstance(javaCoreEnvironment.getProject()).createFileFromText(
                "test.java", JavaLanguage.INSTANCE, text
        );
    }

    @Nullable
    static PsiFile createFile(@NotNull JavaCoreProjectEnvironment javaCoreEnvironment, @NotNull String text) {
        return PsiFileFactory.getInstance(javaCoreEnvironment.getProject()).createFileFromText(
                "test.java", JavaLanguage.INSTANCE, text
        );
    }

    @NotNull
    static JavaCoreProjectEnvironment setUpJavaCoreEnvironment() {
        Disposable parentDisposable = new Disposable() {
            @Override
            public void dispose() {
            }
        };
        JavaCoreApplicationEnvironment applicationEnvironment = new JavaCoreApplicationEnvironment(parentDisposable);
        JavaCoreProjectEnvironment javaCoreEnvironment = new JavaCoreProjectEnvironment(parentDisposable, applicationEnvironment);

        javaCoreEnvironment.addJarToClassPath(findRtJar());
        File annotations = findAnnotations();
        if (annotations != null && annotations.exists()) {
            javaCoreEnvironment.addJarToClassPath(annotations);
        }
        return javaCoreEnvironment;
    }

    @NotNull
    static String prettify(@Nullable String code) {
        if (code == null) {
            return "";
        }
        return code
                .trim()
                .replaceAll("\r\n", "\n")
                .replaceAll(" \n", "\n")
                .replaceAll("\n ", "\n")
                .replaceAll("\n+", "\n")
                .replaceAll(" +", " ")
                .trim()
                ;
    }

    @Nullable
    private static File findRtJar() {
        String javaHome = System.getenv("JAVA_HOME");
        File rtJar;
        if (javaHome == null) {
            rtJar = findActiveRtJar(true);

            if (rtJar == null) {
                throw new SetupJavaCoreEnvironmentException("JAVA_HOME environment variable needs to be defined");
            }
        } else {
            rtJar = findRtJar(javaHome);
        }

        if (rtJar == null || !rtJar.exists()) {
            rtJar = findActiveRtJar(true);

            if ((rtJar == null || !rtJar.exists())) {
                throw new SetupJavaCoreEnvironmentException("No rt.jar found under JAVA_HOME=" + javaHome);
            }
        }
        return rtJar;
    }

    @Nullable
    private static File findRtJar(String javaHome) {
        File rtJar = new File(javaHome, "jre/lib/rt.jar");
        if (rtJar.exists()) {
            return rtJar;
        }

        File classesJar = new File(new File(javaHome).getParentFile().getAbsolutePath(), "Classes/classes.jar");
        if (classesJar.exists()) {
            return classesJar;
        }
        return null;
    }

    @Nullable
    private static File findAnnotations() {
        ClassLoader classLoader = JavaToKotlinTranslator.class.getClassLoader();
        while (classLoader != null) {
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader loader = (URLClassLoader) classLoader;
                for (URL url : loader.getURLs())
                    if ("file".equals(url.getProtocol()) && url.getFile().endsWith("/annotations.jar")) {
                        return new File(url.getFile());
                    }
            }
            classLoader = classLoader.getParent();
        }
        return null;
    }

    @Nullable
    private static File findActiveRtJar(boolean failOnError) {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        if (systemClassLoader instanceof URLClassLoader) {
            URLClassLoader loader = (URLClassLoader) systemClassLoader;
            for (URL url : loader.getURLs()) {
                if ("file".equals(url.getProtocol())) {
                    if (url.getFile().endsWith("/lib/rt.jar")) {
                        return new File(url.getFile());
                    }
                    if (url.getFile().endsWith("/Classes/classes.jar")) {
                        return new File(url.getFile()).getAbsoluteFile();
                    }
                }
            }
            if (failOnError) {
                throw new SetupJavaCoreEnvironmentException("Could not find rt.jar in system class loader: " + StringUtil.join(loader.getURLs(), new Function<URL, String>() {
                    @NotNull
                    @Override
                    public String fun(@NotNull URL url) {
                        return url.toString() + "\n";
                    }
                }, ", "));
            }
        } else if (failOnError) {
            throw new SetupJavaCoreEnvironmentException("System class loader is not an URLClassLoader: " + systemClassLoader);
        }
        return null;
    }

    static void setClassIdentifiers(@NotNull Converter converter, @NotNull PsiElement psiFile) {
        ClassVisitor c = new ClassVisitor();
        psiFile.accept(c);
        converter.clearClassIdentifiers();
        converter.setClassIdentifiers(c.getClassIdentifiers());
    }

    @NotNull
    static String generateKotlinCode(@NotNull String javaCode) {
        PsiFile file = createFile(javaCode);
        if (file != null && file instanceof PsiJavaFile) {
            setClassIdentifiers(CONVERTER, file);
            return prettify(CONVERTER.fileToFile((PsiJavaFile) file).toKotlin());
        }
        return "";
    }

    @NotNull
    static String generateKotlinCodeWithCompatibilityImport(@NotNull String javaCode) {
        PsiFile file = createFile(javaCode);
        if (file != null && file instanceof PsiJavaFile) {
            setClassIdentifiers(CONVERTER, file);
            return prettify(CONVERTER.fileToFileWithCompatibilityImport((PsiJavaFile) file).toKotlin());
        }
        return "";
    }

    private static String readFile(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

    private static void convertFileToKotlin(File file) {
        final PrintStream out = System.out;
        String kotlinCode = "";
        try {
            kotlinCode = generateKotlinCode(readFile(file));
        } catch (Exception e) {
            out.println("EXCEPTION: " + e.getMessage());
        }
        if (kotlinCode.isEmpty()) {
            out.println("EXCEPTION: generated code is empty.");
        } else {
            try {
                File newFile = new File(file.getAbsolutePath().replace(".java", ".kt"));
                BufferedWriter outBuffer = new BufferedWriter(new FileWriter(newFile));
                outBuffer.write(kotlinCode);
                outBuffer.close();
            } catch (IOException e) {
                System.out.println("Exception ");
            }
        }
    }

    public static void convertDirectory(File dir) {
        if (!dir.isDirectory()) {
            System.out.println("EXCEPTION: Given file is not a directory.");
        }
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                convertDirectory(f);
            } else {
                convertFileToKotlin(f);
            }
        }
    }


    public static void main(@NotNull String[] args) {
        //noinspection UseOfSystemOutOrSystemErr
        final PrintStream out = System.out;
        if (args.length == 2) {
            String inputMode = args[0];
            if (inputMode.equals("-code")) {
                String kotlinCode = "";
                try {
                    kotlinCode = generateKotlinCode(args[1]);
                } catch (Exception e) {
                    out.println("EXCEPTION: " + e.getMessage());
                }
                if (kotlinCode.isEmpty()) {
                    out.println("EXCEPTION: generated code is empty.");
                } else {
                    out.println(kotlinCode);
                }
            } else if (inputMode.equals("-file")) {
                convertFileToKotlin(new File(args[1]));
            } else if (inputMode.equals("-directory")) {
                convertDirectory(new File(args[1]));

            } else {
                out.println("EXCEPTION: First argument invalid (should be -code, -file or -directory)");
            }

        } else {
            out.println("EXCEPTION: wrong number of arguments (should be 2).");
        }
    }

    public static String translateToKotlin(String code) {
        return generateKotlinCode(code);
    }
}
