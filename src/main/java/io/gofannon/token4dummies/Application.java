package io.gofannon.token4dummies;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class Application {
    public static void main(String[] args) {
        System.out.println("Starting application");
        new Application().launch();
    }

    private final TokenController tokenController = new TokenController();
    private Terminal terminal;

    private void launch() {
        try {

            terminal = TerminalBuilder.terminal();

            if (tryAcquireToken()) {
                runAsMaster();
            } else {
                runAsSlave();
            }

        } catch (IOException ex) {
            System.err.println("Failure: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            System.out.println("Release token controller");
            tokenController.close();
        }
    }


    private void runAsMaster() throws IOException {
        System.out.println("Starting as master");
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter("write", "read", "exit", "help"))
                .build();

        while (true) {
            try {
                String line = formatLine(reader.readLine("$ "));

                if (!handleMasterCommand(line))
                    handleInvalid(line);

            } catch (UserInterruptException ignored) {
                // Ignore
            } catch (EndOfFileException ignored) {
                return;
            }
        }
    }

    private boolean handleMasterCommand(String line) throws IOException {
        return isEmpty(line)
                || handleExit(line)
                || handleMasterHelp(line)
                || handleWriteToken(line)
                || handleReadToken(line);
    }

    private static boolean isEmpty(String line) {
        return line == null || line.trim().isEmpty();
    }

    private boolean handleExit(String line) {
        if ("exit".equals(line)) {
            System.out.println("Exiting application");
            System.exit(0);
        }
        return false;
    }

    private boolean handleMasterHelp(String line) {
        if ("help".equals(line)) {
            System.out.println("   available commands:");
            System.out.println("      help  : are you kidding ?");
            System.out.println("      exit  : exit application");
            System.out.println("      write : write a content into the token");
            System.out.println("      read  : read and display the content of the token");
            return true;
        }
        return false;
    }


    private boolean handleWriteToken(String line) throws IOException {
        if (line.startsWith("write")) {
            String content = line.substring("write".length());
            tokenController.write(content.trim());
            System.out.println("   Content: '" + content + "'");
            return true;
        }
        return false;
    }


    private boolean handleReadToken(String line) throws IOException {
        if ("read".equals(line)) {
            String content = readToken();
            System.out.println("   Content: '" + content + "'");
            return true;
        }
        return false;
    }


    private String readToken() throws IOException {
        return tokenController.read();
    }

    private void handleInvalid(String line) {
        System.err.println("Invalid command: '" + line + "'");
    }

    private boolean runSlaveAsMaster = false;


    private void runAsSlave() throws IOException {
        System.out.println("Starting as slave");

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter("retry", "read", "exit", "help"))
                .build();

        while (true) {
            try {

                String line = formatLine(reader.readLine("$ "));
                if (handleRetryAcquireToken(line)) {
                    if( runSlaveAsMaster)
                        break;
                } else if (!handleSlaveCommand(line)) {
                    handleInvalid(line);
                }

            } catch (UserInterruptException ignored) {
                // Ignore
            } catch (EndOfFileException ignored) {
                return;
            }
        }
        if (runSlaveAsMaster) {
            runAsMaster();
        }
    }

    private String formatLine(String line) {
        if (line == null)
            return "";
        return line.trim();
    }

    private boolean handleSlaveCommand(String line) throws IOException {
        return isEmpty(line)
                || handleExit(line)
                || handleReadToken(line)
                || handleSlaveHelp(line);
    }

    private boolean handleRetryAcquireToken(String line) {
        if (!"retry".equals(line))
            return false;

        System.out.println("Re-trying acquire token");
        if (retryAcquireToken()) {
            System.out.println("   lock acquired");
            runSlaveAsMaster = true;
        } else {
            System.out.println("   lock NOT acquired");
        }
        return true;
    }


    private boolean handleSlaveHelp(String line) {
        if ("help".equals(line)) {
            System.out.println("   available commands:");
            System.out.println("      help  : are you kidding ?");
            System.out.println("      exit  : exit application");
            System.out.println("      read  : read and display the content of the token");
            System.out.println("      retry : retry to acquire the token");
            return true;
        }
        return false;
    }


    private boolean tryAcquireToken() {
        try {

            tokenController.acquireWriteToken();
            return tokenController.isWriteLocked();

        } catch (IOException ex) {
            return false;
        }
    }

    private boolean retryAcquireToken() {
        try {
            tokenController.retryAcquireWriteToken();
            return tokenController.isWriteLocked();
        } catch (IOException ex) {
            return false;
        }
    }
}
