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

package samples.testbed.scanner;

import com.ib.client.ScannerSubscription;

public class ScannerSubscriptionSamples {
	
	public static ScannerSubscription HotUSStkByVolume() {
		
		//! [hotusvolume]
        //Hot US stocks by volume
		ScannerSubscription scanSub = new ScannerSubscription();
		scanSub.instrument("STK");
		scanSub.locationCode("STK.US.MAJOR");
		scanSub.scanCode("HOT_BY_VOLUME");
		//! [hotusvolume]
        return scanSub;
		
	}
	
	public static ScannerSubscription TopPercentGainersIbis() {
		
		//! [toppercentgaineribis]
        //Top % gainers at IBIS
        ScannerSubscription scanSub = new ScannerSubscription();
        scanSub.instrument("STOCK.EU");
        scanSub.locationCode("STK.EU.IBIS");
        scanSub.scanCode("TOP_PERC_GAIN");
      //! [toppercentgaineribis]
        return scanSub;
		
	}
	
    public static ScannerSubscription MostActiveFutEurex()
    {
        //! [mostactivefuteurex]
        //Most active futures at EUREX
        ScannerSubscription scanSub = new ScannerSubscription();
        scanSub.instrument("FUT.EU");
        scanSub.locationCode("FUT.EU.EUREX");
        scanSub.scanCode("MOST_ACTIVE");
        //! [mostactivefuteurex]
        return scanSub;
    }

	public static ScannerSubscription HighOptVolumePCRatioUSIndexes() {
		
		//! [highoptvolume]
        //High option volume P/C ratio US indexes
        ScannerSubscription scanSub = new ScannerSubscription();
        scanSub.instrument("IND.US");
        scanSub.locationCode("IND.US");
        scanSub.scanCode("HIGH_OPT_VOLUME_PUT_CALL_RATIO");
        //! [highoptvolume]
        return scanSub;
		
	}

	public static ScannerSubscription ComplexOrdersAndTrades() {

	    //! [combolatesttrade]
        //Complex orders and trades scan, latest trades
        ScannerSubscription scanSub = new ScannerSubscription();
        scanSub.instrument("NATCOMB");
        scanSub.locationCode("NATCOMB.OPT.US");
        scanSub.scanCode("COMBO_LATEST_TRADE");
        //! [combolatesttrade]
        return scanSub;

    }


}
