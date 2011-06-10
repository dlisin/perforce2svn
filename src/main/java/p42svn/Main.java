package p42svn;

import org.apache.commons.cli.*;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

/**
 * @author Pavel Belevich
 *         Date: 5/15/11
 *         Time: 2:05 AM
 */
public class Main {

    @SuppressWarnings({"AccessStaticViaInstance", "unchecked", "EmptyCatchBlock"})
    public static void main(String[] args) {

        Options options = new Options();

        options.addOption("help", false, "Print detailed help message and exit.");
        options.addOption(OptionBuilder.
                withArgName("p4_depot_spec=svn_path").
                hasArgs(2).
                withValueSeparator().
                withDescription("Specify mapping of Perforce branch to repository path. Takes an " +
                        "argument of the form p4_depot_spec=svn_path. Multiple branch " +
                        "mappings may be specified, but at least one is required.").
                create("branches"));
        options.addOption(OptionBuilder.withArgName("path")
                .hasArg()
                .withDescription("Path to the directory where participial dumps for each changelist will be stored. Default path is ./tmp")
                .create("dumpDir"));
        options.addOption(OptionBuilder.withArgName("path")
                .hasArg()
                .withDescription("Path to result dump file. Default path is ./dump")
                .create("dumpFile"));
        options.addOption(OptionBuilder.withArgName("number")
                .hasArg()
                .withDescription("Number of simultaneously processed changelists. Default value is 10.")
                .withType(10)
                .create("threads"));
        options.addOption(OptionBuilder.withArgName("host:port")
                .hasArg()
                .withDescription("Specify Perforce server and port; this overrides $P4PORT in the" +
                        "environment.")
                .create("port"));
        options.addOption(OptionBuilder.withArgName("username")
                .hasArg()
                .withDescription("Specify Perforce username; this overrides $P4USER in the environment.")
                .create("user"));
        options.addOption(OptionBuilder.withArgName("password")
                .hasArg()
                .withDescription("Specify Perforce password; this overrides $P4PASSWD in the environment.")
                .create("password"));
        options.addOption(OptionBuilder.withArgName("client")
                .hasArg()
                .withDescription("Specify Perforce client; this overrides $P4CLIENT in the environment.")
                .create("client"));
        options.addOption("addp4info", false, "Add original Perforce changlist id to Subversion revision description.");
        options.addOption(OptionBuilder.withArgName("template")
                .hasArg()
                .withDescription("String template for original Perforce changlist id for adding to Subversion revision description.")
                .create("p4info"));
        options.addOption(OptionBuilder.withArgName("TMZ")
                .hasArg()
                .withDescription("Perforce server timezone, e.g. \"GMT-8\", \"America/Los_Angeles\".")
                .create("timezone"));
        options.addOption("dump", false, "Only dump files into dump folder without assembling dump file.");
        options.addOption("assemble", false, "Only assemble dump file from already dumped files from dump folder.");
        options.addOption(OptionBuilder.withArgName("CHARSET")
                .hasArg()
                .withDescription("Output files charset. Default is platform default.")
                .create("charset"));

        options.addOption("fromChangeList", true, "Process only changelists with greater then given number");
        options.addOption("toChangeList", true, "Process only changelists with less of equal to given number");
        options.addOption("prevDumpDir", true, "Previous dump dir needed to continue import");
        options.addOption("restore", false, "Restore from previous failure, do not clear dump dir, reuse files already processed");

        options.addOption(OptionBuilder.withArgName("N")
                .hasArg()
                .withDescription("Split dump file by N parts.")
                .create("splitBy"));


        CommandLineParser parser = new PosixParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help") || args.length == 0) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("p42svn", options);
            }

            if (cmd.hasOption("branches")) {
                Properties branches = cmd.getOptionProperties("branches");
                P42SVN p42SVN = new P42SVN();
                HashMap hashMap = new HashMap();
                hashMap.putAll(branches);
                p42SVN.setBranches(hashMap);

                String dumpDir = "tmp";
                if (cmd.hasOption("dumpDir")) {
                    dumpDir = cmd.getOptionValue("dumpDir");
                }
                p42SVN.setChangelistsDumpDirectoryPath(dumpDir);

                int threads = 10;
                if (cmd.hasOption("threads")) {
                    String threadsString = cmd.getOptionValue("threads");
                    try {
                        threads = Integer.valueOf(threadsString);
                    } catch (NumberFormatException e) {

                    }

                }
                p42SVN.setSimultaneousProcessedChangelistCount(threads);

                String dumpFile = "dump";
                if (cmd.hasOption("dumpFile")) {
                    dumpFile = cmd.getOptionValue("dumpFile");
                }
                p42SVN.setDumpFileName(dumpFile);

                Map<String, String> env = System.getenv();

                String p4port = env.get("P4PORT");
                if (cmd.hasOption("port")) {
                    p4port = cmd.getOptionValue("port");
                }
                p42SVN.getP4().setServerUriString(p4port != null ? "p4java://" + p4port : null);

                String p4user = env.get("P4USER");
                if (cmd.hasOption("user")) {
                    p4user = cmd.getOptionValue("user");
                }
                p42SVN.getP4().setUserName(p4user != null ? p4user : null);

                String password = env.get("P4PASSWD");
                if (cmd.hasOption("password")) {
                    password = cmd.getOptionValue("password");
                }
                p42SVN.getP4().setPassword(password != null ? password : null);

                String client = env.get("P4CLIENT");
                if (cmd.hasOption("client")) {
                    client = cmd.getOptionValue("client");
                }
                p42SVN.getP4().setClientName(client != null ? client : null);

                if (cmd.hasOption("addp4info")) {
                    p42SVN.setAddOriginalChangeListId(true);
                    if (cmd.hasOption("p4info")) {
                        String p4info = cmd.getOptionValue("p4info");
                        p42SVN.setOriginalChangeListInfoString(p4info);
                    }
                }

                if (cmd.hasOption("timezone")) {
                    String timezone = cmd.getOptionValue("timezone");
                    p42SVN.setTimeZone(TimeZone.getTimeZone(timezone));
                }

                Charset charset = Charset.defaultCharset();
                if (cmd.hasOption("charset")) {
                    String charsetString = cmd.getOptionValue("charset");
                    charset = Charset.forName(charsetString);
                }
                p42SVN.setCharset(charset);
                p42SVN.setRestoreMode(cmd.hasOption("restore"));
                boolean dump = true;
                boolean assemble = true;

                if (cmd.hasOption("dump") || cmd.hasOption("assemble")) {
                    dump = cmd.hasOption("dump");
                    assemble = cmd.hasOption("assemble");
                }

                if (cmd.hasOption("fromChangeList")) {
                    p42SVN.setFromChangeList(Integer.parseInt(cmd.getOptionValue("fromChangeList")));
                }
                if (cmd.hasOption("toChangeList")) {
                    p42SVN.setToChangeList(Integer.parseInt(cmd.getOptionValue("toChangeList")));
                }
                if (cmd.hasOption("prevDumpDir")) {
                    p42SVN.setPreviousDumpPath(cmd.getOptionValue("prevDumpDir"));
                    p42SVN.applyPropertiesFromPreviousDump();
                }

                if (cmd.hasOption("splitBy")) {
                    p42SVN.setSplitBy(Integer.parseInt(cmd.getOptionValue("splitBy")));
                }


                try {
                    if (dump) p42SVN.getP4().getServer(true);
                    p42SVN.getEventDispatcher().addListener(new SVNListener(p42SVN));
                    if (dump && !p42SVN.isRestoreMode()) p42SVN.clearDumpDirectory();
                    if (dump) p42SVN.dumpChangeLists();
                    if (assemble) p42SVN.assembleDumpFile();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        } catch (Exception exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
            exp.printStackTrace(System.out);
        }
    }

}
