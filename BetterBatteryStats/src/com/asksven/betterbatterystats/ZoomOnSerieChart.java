
package com.asksven.betterbatterystats;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.BubbleChart;
import org.achartengine.chart.LineChart;
import org.achartengine.chart.PointStyle;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;
import com.asksven.betterbatterystats_xdaedition.R;

/**
 * Combined temperature demo chart.
 */
public class ZoomOnSerieChart extends AbstractChart
{
	private BatteryGraphSeries m_serie;
	
	public ZoomOnSerieChart(BatteryGraphSeries serie)
	{
		m_serie = serie;
	}
	/**
	 * Returns the chart name.
	 * 
	 * @return the chart name
	 */
	public String getName()
	{
		return m_serie.getTitle();
	}

	/**
	 * Executes the chart
	 * @param context the context
	 * @return the built intent
	 */
	public Intent execute(Context context)
	{
		String[] titles = new String[]
		{ m_serie.getTitle() };
		List<Date[]> dates = new ArrayList<Date[]>();
		List<double[]> values = new ArrayList<double[]>();

		int length = titles.length;
		for (int i = 0; i < length; i++)
		{
			dates.add(new Date[m_serie.size()]);
			for (int j=0; j < m_serie.size(); j++)
			{
				dates.get(i)[j] = new Date((Long) m_serie.getX(j));
			}
		}

		values.add(new double[m_serie.size()]);
		
		for (int i=0; i < m_serie.size(); i++)
		{
			values.get(0)[i] = m_serie.getY(i).doubleValue();
		}
		
		length = values.get(0).length;
		int[] colors = new int[]
		{ Color.BLUE };

		PointStyle[] styles = new PointStyle[]
		{ PointStyle.POINT };

		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		//XYMultipleSeriesRenderer renderer = buildBarRenderer(colors);
		setChartSettings(renderer, m_serie.getTitle(), "Time", "",
				dates.get(0)[0].getTime(), dates.get(0)[m_serie.size()-1].getTime(), 0, 1,
				Color.GRAY, Color.LTGRAY);
		renderer.setXLabels(5);
		renderer.setYLabels(1);
	    renderer.setShowGrid(true);
	    renderer.setZoomButtonsVisible(true);
	    renderer.setPanLimits(new double[] { dates.get(0)[0].getTime(), dates.get(0)[m_serie.size()-1].getTime(), 0, 1 });
	    renderer.setZoomLimits(new double[] { dates.get(0)[0].getTime(), dates.get(0)[m_serie.size()-1].getTime(), 0, 1 });

		length = renderer.getSeriesRendererCount();

		for (int i = 0; i < length; i++)
		{
//			SimpleSeriesRenderer seriesRenderer = renderer
//					.getSeriesRendererAt(i);
//			seriesRenderer.setDisplayChartValues(true);
			XYSeriesRenderer seriesRenderer = (XYSeriesRenderer) renderer.getSeriesRendererAt(i);
		      seriesRenderer.setFillBelowLine(true);
		      seriesRenderer.setFillBelowLineColor(colors[i]);
		      //seriesRenderer.setLineWidth(2.5f);
		      seriesRenderer.setDisplayChartValues(false);
		      //seriesRenderer.setChartValuesTextSize(10f);

			
		}
		return ChartFactory
				.getTimeChartIntent(context,
						buildDateDataset(titles, dates, values), renderer,
						"HH:mm:ss");
	}

}