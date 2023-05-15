package de.silpion.html2text;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class App {
    private static final String CRLF = "\r\n";
    public static final Charset ENCODING = StandardCharsets.UTF_8;

    public static void main(String[] args) throws IOException {
        new App().process(args);
    }

    private void process(String[] args) throws IOException {
        boolean dryrun = false;
        int pathArg = 0;
        if (args.length != 1 && args.length != 2) {
            System.out.println("this applicaion needs one or two arguments");
            printHelp();
            System.exit(-1);
        }

        if (args.length == 2) {
            if (!"-dry".equals(args[0])) {
                System.out.println("unknown first argument: '" + args[0] + "'");
                printHelp();
                System.exit(-2);
            } else {
                dryrun = true;
                pathArg = 1;
            }

        }
        File f = new File(args[pathArg]);
        if (!f.exists()) {
            System.out.println("the file/folder '" + f.getAbsolutePath() + "' does not exist");
            printHelp();
            System.exit(-3);
        }

        if (f.isFile()) {
            processFile(f, createTextFileFromHtmlFile(f), dryrun);
        } else {
            process(f.getAbsolutePath(), dryrun);
        }
    }

    private void printHelp() {
        System.out.println("<app> [-dry] <pathToDirectory>");
    }

    private File createTextFileFromHtmlFile(File htmlFile) {
        String name = FilenameUtils.getBaseName(htmlFile.getName());
        return new File(htmlFile.getParent(), name + ".txt");
    }

    public void process(String path, boolean dryRun) throws IOException {

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            paths
                    .map(p -> p.toFile())
                    .filter(f -> f.getName().toLowerCase().endsWith(".html"))
                    .forEach(f -> {
                        try {
                            processFile(f, createTextFileFromHtmlFile(f), dryRun);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    public void processFile(File htmlFile, File textFile, boolean dryRun) throws IOException {
        System.out.println("---------------------------------------------------------------------------------");
        System.out.println(htmlFile.getAbsolutePath());
        System.out.println(" -> ");
        System.out.println(textFile.getAbsolutePath());
        System.out.println("---------------------------------------------------------------------------------");
        String html = FileUtils.readFileToString(htmlFile, ENCODING);
        Document doc = Jsoup.parse(html);
        String text = text(doc);
        System.out.println(text);
        if (!dryRun) {
            FileUtils.write(textFile, text, ENCODING);
        }
    }

    public String text(Document doc) {
        final StringBuilder sb = new StringBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            public void head(Node node, int depth) {
                if (node instanceof TextNode) {
                    TextNode textNode = (TextNode) node;
                    String text = getNormalisedText(textNode).trim();
                    if (StringUtils.isNotBlank(text)) {
                        sb.append(text).append(CRLF);
                    }
                }
            }

            public void tail(Node node, int depth) {
            }
        }, doc);
        return sb.toString();
    }

    private static String getNormalisedText(TextNode textNode) {
        String text = textNode.getWholeText();
        if (preserveWhitespace(textNode.parent())) {
            return text;
        } else {
            return getNormalisedWhitespace(text, lastCharIsWhitespace(text));
        }
    }

    public static String getNormalisedWhitespace(String string, boolean stripLeading) {
        boolean lastWasWhite = false;
        boolean reachedNonWhite = false;
        int len = string.length();

        StringBuilder sb = new StringBuilder();
        int c;
        for (int i = 0; i < len; i += Character.charCount(c)) {
            c = string.codePointAt(i);
            if (isActuallyWhitespace(c)) {
                if ((!stripLeading || reachedNonWhite) && !lastWasWhite) {
                    sb.append(' ');
                    lastWasWhite = true;
                }
            } else if (!isInvisibleChar(c)) {
                sb.appendCodePoint(c);
                lastWasWhite = false;
                reachedNonWhite = true;
            }
        }

        return sb.toString();
    }

    public static boolean isActuallyWhitespace(int c) {
        return c == 32 || c == 9 || c == 10 || c == 12 || c == 13 || c == 160;
    }

    public static boolean isInvisibleChar(int c) {
        return Character.getType(c) == 16 && (c == 8203 || c == 8204 || c == 8205 || c == 173);
    }

    static boolean preserveWhitespace(Node node) {
        if (node != null && node instanceof Element) {
            Element el = (Element) node;
            int i = 0;

            do {
                if (el.tag().preserveWhitespace()) {
                    return true;
                }

                el = el.parent();
                ++i;
            } while (i < 6 && el != null);
        }

        return false;
    }

    static boolean lastCharIsWhitespace(String s) {
        return s.length() != 0 && s.charAt(s.length() - 1) == ' ';
    }
}
