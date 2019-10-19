package Matching.Implementations;

import Matching.ControlMatcher;
import Models.GroupAddress;
import Models.OpenHAB.KnxControl;
import Models.OpenHAB.SwitchControl;

import java.util.LinkedList;
import java.util.List;

public class SwitchMatcher implements ControlMatcher {

    private final String controlDesignation;
    private final String statusDesignation;

    public SwitchMatcher(String controlDesignation, String statusDesignation) {
        this.controlDesignation = controlDesignation;
        this.statusDesignation = statusDesignation;
    }

    @Override
    public List<KnxControl> ExtractControls(List<GroupAddress> addresses) {

        var onOffAddresses = new LinkedList<GroupAddress>();
        var statusAddresses = new LinkedList<GroupAddress>();


        for (var groupAddress : addresses) {

            if (!groupAddress.equalsDataPoint("DPST-1-1")) {
                continue;
            }

            if (org.apache.commons.lang3.StringUtils.containsIgnoreCase(groupAddress.getName(), statusDesignation)) {

                statusAddresses.add(groupAddress.clone());
            } else if (org.apache.commons.lang3.StringUtils.containsIgnoreCase(groupAddress.getName(), controlDesignation)) {

                onOffAddresses.add(groupAddress.clone());
            }
        }

        onOffAddresses.stream().forEach(address -> address.setName(address.getName().replace(controlDesignation, "").trim()));
        statusAddresses.stream().forEach(address -> address.setName(address.getName().replace(statusDesignation, "").trim()));

        var resultControls = BuildControlsFromAddresses(onOffAddresses, statusAddresses, addresses);

        return resultControls;
    }

    private List<KnxControl> BuildControlsFromAddresses(List<GroupAddress> controlAddresses, List<GroupAddress> statusAddresses, List<GroupAddress> all) {
        var resultControls = new LinkedList<KnxControl>();


        var iter = statusAddresses.listIterator();
        while (iter.hasNext()) {

            var statusAddress = iter.next();
            var controlAddress = validateGroupAddress(statusAddress, statusDesignation, controlAddresses);

            if (controlAddress != null) {
                var switchControl = new SwitchControl(statusAddress.getName().replace(statusDesignation, "").trim());
                switchControl.setWriteAddress(controlAddress);
                switchControl.setReadAddress(statusAddress);

                resultControls.add(switchControl);

                all.remove(statusAddress);
                all.remove(controlAddress);

                controlAddresses.remove(controlAddress);
                iter.remove();
            }
        }

        iter = controlAddresses.listIterator();
        while (iter.hasNext()) {
            var controlAddress = iter.next();
            var statusAddress = validateGroupAddress(controlAddress, controlDesignation, controlAddresses);

            var switchControl = new SwitchControl(controlAddress.getName().replace(controlDesignation, "").trim());
            switchControl.setWriteAddress(controlAddress);

            if (statusAddress != null) {
                switchControl.setReadAddress(controlAddress);

                resultControls.add(switchControl);

                all.remove(statusAddress);

                statusAddresses.remove(controlAddress);
            }
            iter.remove();
            all.remove(controlAddress);

            resultControls.add(switchControl);
        }

        return resultControls;
    }

    private GroupAddress validateGroupAddress(GroupAddress statusAddress, String designation, List<GroupAddress> groupAddresses) {
        var nameIdentifier = statusAddress.getName().replace(designation, "").trim();

        return groupAddresses.stream()
                .filter(ad -> org.apache.commons.lang3.StringUtils.containsIgnoreCase(ad.getName(), nameIdentifier))
                .findAny().orElse(null);
    }


}
