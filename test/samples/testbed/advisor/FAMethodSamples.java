/*
 * Java TWS API Samples
 *
 * Copyright (C) 2013-2026  Interactive Brokers LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package samples.testbed.advisor;

public class FAMethodSamples {
    //! [faupdatedgroup]
    public static final String FA_UPDATED_GROUP = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<ListOfGroups>" +
            "<Group>" +
                "<name>MyTestProfile1</name>" +
                "<defaultMethod>ContractsOrShares</defaultMethod>" +
                "<ListOfAccts varName=\"list\">" +
                    "<Account>" +
                        "<acct>DU6202167</acct>" +
                        "<amount>100.0</amount>" +
                        "</Account>" +
                    "<Account>" +
                        "<acct>DU6202168</acct>" +
                        "<amount>200.0</amount>" +
                    "</Account>" +
                "</ListOfAccts>" +
            "</Group>" +
            "<Group>" +
                "<name>MyTestProfile2</name>" +
                "<defaultMethod>Ratio</defaultMethod>" +
                "<ListOfAccts varName=\"list\">" +
                    "<Account>" +
                        "<acct>DU6202167</acct>" +
                        "<amount>1.0</amount>" +
                    "</Account>" +
                    "<Account>" +
                        "<acct>DU6202168</acct>" +
                        "<amount>2.0</amount>" +
                    "</Account>" +
                "</ListOfAccts>" +
            "</Group>" +
            "<Group>" +
                "<name>MyTestProfile3</name>" +
                "<defaultMethod>Percent</defaultMethod>" +
                "<ListOfAccts varName=\"list\">" +
                    "<Account>" +
                        "<acct>DU6202167</acct>" +
                        "<amount>60.0</amount>" +
                    "</Account>" +
                    "<Account>" +
                        "<acct>DU6202168</acct>" +
                        "<amount>40.0</amount>" +
                    "</Account>" +
                "</ListOfAccts>" +
            "</Group>" +
            "<Group>" +
            "<name>MyTestProfile4</name>" +
                "<defaultMethod>MonetaryAmount</defaultMethod>" +
                "<ListOfAccts varName=\"list\">" +
                    "<Account>" +
                        "<acct>DU6202167</acct>" +
                        "<amount>1000.0</amount>" +
                    "</Account>" +
                    "<Account>" +
                        "<acct>DU6202168</acct>" +
                        "<amount>2000.0</amount>" +
                    "</Account>" +
                "</ListOfAccts>" +
            "</Group>" +
            "<Group>" +
                "<name>Group_1</name>" +
                "<defaultMethod>NetLiq</defaultMethod>" +
                "<ListOfAccts varName=\"list\">" +
                    "<Account>" +
                        "<acct>DU6202167</acct>" +
                    "</Account>" +
                    "<Account>" +
                        "<acct>DU6202168</acct>" +
                    "</Account>" +
                "</ListOfAccts>" +
            "</Group>" +
            "<Group>" +
                "<name>MyTestGroup1</name>" +
                "<defaultMethod>AvailableEquity</defaultMethod>" +
                "<ListOfAccts varName=\"list\">" +
                    "<Account>" +
                        "<acct>DU6202167</acct>" +
                    "</Account>" +
                    "<Account>" +
                        "<acct>DU6202168</acct>" +
                    "</Account>" +
                "</ListOfAccts>" +
            "</Group>" +
            "<Group>" +
                "<name>MyTestGroup2</name>" +
                "<defaultMethod>NetLiq</defaultMethod>" +
                "<ListOfAccts varName=\"list\">" +
                    "<Account>" +
                        "<acct>DU6202167</acct>" +
                    "</Account>" +
                    "<Account>" +
                        "<acct>DU6202168</acct>" +
                    "</Account>" +
                "</ListOfAccts>" +
            "</Group>" +
            "<Group>" +
                "<name>MyTestGroup3</name>" +
                "<defaultMethod>Equal</defaultMethod>" +
                "<ListOfAccts varName=\"list\">" +
                    "<Account>" +
                        "<acct>DU6202167</acct>" +
                    "</Account>" +
                    "<Account>" +
                        "<acct>DU6202168</acct>" +
                    "</Account>" +
                "</ListOfAccts>" +
            "</Group>" +
            "<Group>" +
                "<name>Group_2</name>" +
                "<defaultMethod>AvailableEquity</defaultMethod>" +
                "<ListOfAccts varName=\"list\">" +
                    "<Account>" +
                        "<acct>DU6202167</acct>" +
                    "</Account>" +
                    "<Account>" +
                        "<acct>DU6202168</acct>" +
                    "</Account>" +
                "</ListOfAccts>" +
            "</Group>" +
        "</ListOfGroups>";	
    //! [faupdatedgroup]
}
