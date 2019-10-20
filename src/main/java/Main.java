import Export.FileExporter;
import Import.EtsImport;
import Import.Importer;
import Matching.Implementations.DimmerMatcher;
import Matching.Implementations.RollerShutterMatcher;
import Matching.Implementations.SwitchMatcher;
import Models.GroupAddress;
import Models.ImportException;
import Models.OpenHAB.KnxControl;
import Parser.GroupAddressFactory;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.nio.file.Paths;
import java.util.LinkedList;

public class Main {

    public static void main(String[] args) {

        if (args.length != 2) {

            System.out.println("USAGE: Main etsproject.knxproj openhab/conf/");
            System.exit(0);
        }

        var importPath = args[0];
        var confDirectoryString = args[1];
        var confDirectory = Paths.get(confDirectoryString);

        EtsImport etsImporter = new Importer();

        try {
            var doc = etsImporter.ImportEtsFile(importPath);

            XPath xPath =  XPathFactory.newInstance().newXPath();

            String expression = "//GroupAddress";
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

            System.out.println(String.format("Xpath \"%s\" returned \"%d\" elements", expression, nodeList.getLength()));

            var groupAddressList = new LinkedList<GroupAddress>();

            for (int i = 0; i < nodeList.getLength(); i++) {
                var node = nodeList.item(i);

                var groupAddress = GroupAddressFactory.CreateGroupAddressFromNode(node);

                if (groupAddress != null) {
                    groupAddressList.add(groupAddress);
                }
            }

            System.out.println(String.format("Xpath \"%s\" returned \"%d\" elements", expression, nodeList.getLength()));
            System.out.println(String.format("Parsed \"%d\" nodes successfully", groupAddressList.size()));


            var switchMatcher = SwitchMatcher.BuildSwitchMatcher("LI", "RM LI");
            var rollerShutterMatcher = RollerShutterMatcher.BuildRollershutterMatcher("LZ", "", "WE HÖ", "RM WE HÖ", "SP", "KZ");
            var dimmerMatcher = DimmerMatcher.BuildDimmerMatcher("LI", "RM LI", "WE", "RM WE", "DIM");

            var controls = new LinkedList<KnxControl>();

            System.out.printf("Group address count before dimmer extraction: %d\n", groupAddressList.size());
            var dimmer = dimmerMatcher.ExtractControls(groupAddressList);
            System.out.printf("Group address count after dimmer extraction: %d\n", groupAddressList.size());
            System.out.printf("Number of dimmer: %d\n", dimmer.size());


            System.out.printf("Group address count before switch extraction: %d\n", groupAddressList.size());
            var switches = switchMatcher.ExtractControls(groupAddressList);
            System.out.printf("Group address count after switch extraction: %d\n", groupAddressList.size());
            System.out.printf("Number of switches: %d\n", switches.size());


            System.out.printf("Group address count before rollerShutter extraction: %d\n", groupAddressList.size());
            var rollerShutters = rollerShutterMatcher.ExtractControls(groupAddressList);
            System.out.printf("Group address count after rollerShutter extraction: %d\n", groupAddressList.size());
            System.out.printf("Number of rollerShutters: %d\n", rollerShutters.size());


            controls.addAll(dimmer);

            controls.addAll(rollerShutters);

            controls.addAll(switches);

            FileExporter.WriteImportedConfiguration(confDirectory, controls, "knximport");

        } catch (ImportException | XPathExpressionException e) {
            e.printStackTrace();
        }

    }


}
