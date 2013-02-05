package org.jetbrains.jet.j2k;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.Assert.*;

public class KJ2KArgsTest {

    @BeforeClass
    public static void classSetup() {
    }

    @Before
    public void testSetup() {
    }

    @Test
    public void testIfFirstArgInvalid(){
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

        assertEquals(expected,  output.toString().trim());

    }

    @Test
    public void testCodeWorks(){
        String expected = "open class Main() {\n" +
                "class object {\n" +
                "public open fun main(args : Array<String?>?) : Unit {\n" +
                "System.out?.println(\"FOO\")\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "fun main(args : Array<String?>?) = Main.main(args)";
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
    public void testFileWorks(){
        String expected = "open class Main() {\n" +
                "class object {\n" +
                "public open fun main(args : Array<String?>?) : Unit {\n" +
                "System.out?.println(\"FOO\")\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "fun main(args : Array<String?>?) = Main.main(args)";
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
    public void testDirectoryWorks(){
        String expected = "open class Main() {\n" +
                "class object {\n" +
                "public open fun main(args : Array<String?>?) : Unit {\n" +
                "System.out?.println(\"FOO\")\n" +
                "}\n" +
                "}\n" +
                "}\n" +
                "fun main(args : Array<String?>?) = Main.main(args)";
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