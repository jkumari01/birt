
/*******************************************************************************
 * Copyright (c) 2004, 2010 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.report.designer.data.ui.dataset;


import java.util.Map;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IResultIterator;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.IDatasetPreviewTask;
import org.eclipse.birt.report.engine.api.IExtractionResults;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.impl.ReportEngineFactory;
import org.eclipse.birt.report.model.api.DataSetHandle;

/**
 * 
 */

public class DataSetPreviewer 
{
	private DataSetHandle dataSetHandle;
	private int maxRow;
	
	private IReportEngine engine;
	private IDatasetPreviewTask task;
	private IExtractionResults result;
	
	public void open( Map appContext, EngineConfig config ) throws BirtException
	{
		engine = createReportEngine( config );
		task = engine.createDatasetPreviewTask( );
		task.setMaxRow( maxRow );
		task.setDataSet( dataSetHandle );
		task.setAppContext( appContext );
		ReportParameterUtil.completeParamDefalutValues( task, dataSetHandle.getModuleHandle( ) );
	}
	
	public DataSetPreviewer( DataSetHandle dataSetHandle, int maxRow )
	{
		this.dataSetHandle = dataSetHandle;
		this.maxRow = maxRow;

	}
	
	public IResultIterator preview( ) throws BirtException
	{
		result = task.execute( );
		return result.nextResultIterator( ).getResultIterator( );
	}
	
	
	private static IReportEngine createReportEngine( EngineConfig config ) throws BirtException
	{
		return new ReportEngineFactory( ).createReportEngine( config );
	}

	
	public void close( ) throws BirtException
	{
		if ( result != null )
		{
			result.close( );
		}
		if ( task != null )
		{
			task.close( );
		}
		if ( engine != null )
		{
			engine.destroy( );
		}
	}
}