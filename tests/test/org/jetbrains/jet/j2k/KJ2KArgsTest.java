package org.jetbrains.jet.j2k;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.Scanner;

import static org.junit.Assert.*;

public class KJ2KArgsTest {

    @BeforeClass
    public static void classSetup() {
    }

    @Before
    public void testSetup() {
    }

    @Test(expected = RuntimeException.class)
    public void testIfFirstArgInvalid() throws IOException {
        String expected = "EXCEPTION: First argument invalid (should be -code, -file or -directory)";
        @NotNull
        String args[] = {"-" , "class Main {\n" +
                "  public static void main(String args[]){\n" +
                "    System.out.println(\"FOO\");\n" +
                "  }\n" +
                "}"};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        JavaToKotlinTranslator.main(args);


    }

    @Test
    public void testCodeWorks() throws IOException {
        String expected = "open class Main() {\n" +
                "class object {\n" +
                "public open fun main(args : Array<String?>?) : Unit {\n" +
                "System.out?.println(\"FOO\")\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "fun main(args : Array<String>) = Main.main(args as Array<String?>?)";
        @NotNull
        String args[] = {"-code" , "class Main {\n" +
                "  public static void main(String args[]){\n" +
                "    System.out.println(\"FOO\");\n" +
                "  }\n" +
                "}"};
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        JavaToKotlinTranslator.main(args);

        assertEquals(expected,  output.toString().trim());

    }
    @Test
    public void testFileWorks() throws IOException {
        String expected = "open class Main() {\n" +
                "class object {\n" +
                "public open fun main(args : Array<String?>?) : Unit {\n" +
                "System.out?.println(\"FOO\")\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "fun main(args : Array<String>) = Main.main(args as Array<String?>?)";
        @NotNull
        String args[] = {"-file" , "tests/resources/argsTestData/Main.java"};

        JavaToKotlinTranslator.main(args);
        File f = new File("tests/resources/argsTestData/Main.kt");
        String actual = "";
        Scanner scanner = null;

        try {
            scanner = new Scanner(f);
            while (scanner.hasNextLine()) {
                actual += scanner.nextLine() + "\n";
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            scanner.close();
            f.delete();
        }

        assertEquals(expected, actual.trim());


    }
    @Test
    public void testDirectoryWorks() throws IOException {
        String expected = "open class Main() {\n" +
                "class object {\n" +
                "public open fun main(args : Array<String?>?) : Unit {\n" +
                "System.out?.println(\"FOO\")\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "fun main(args : Array<String>) = Main.main(args as Array<String?>?)";
        @NotNull
        String args[] = {"-directory" , "tests/resources/argsTestData"};
        JavaToKotlinTranslator.main(args);

        File f = new File("tests/resources/argsTestData/Main.kt");
        String actual = "";
        Scanner scanner = null;

        try {
            scanner = new Scanner(f);
            while (scanner.hasNextLine()) {
                actual += scanner.nextLine() + "\n";
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            scanner.close();
            f.delete();
        }

        assertEquals(expected,  actual.trim());
    }


}