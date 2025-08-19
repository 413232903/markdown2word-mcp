package org.apache.poi.xddf.usermodel.chart;

import lombok.val;

public class PoiUtil {
    public static String getTitle(XDDFChartData.Series series) {
        val seriesText = series.getSeriesText();
        if (!seriesText.isSetStrRef()) {
            return null;
        }
        val strRef = seriesText.getStrRef();
        if (!strRef.isSetStrCache()) {
            return null;
        }
        val strCache = strRef.getStrCache();
        if (strCache.sizeOfPtArray() < 1) {
            return null;
        }
        return strCache.getPtArray(0).getV();
    }
}
