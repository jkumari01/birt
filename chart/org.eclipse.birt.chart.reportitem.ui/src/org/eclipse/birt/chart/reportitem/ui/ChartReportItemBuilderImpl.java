/***********************************************************************
 * Copyright (c) 2004, 2007 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Actuate Corporation - initial API and implementation
 ***********************************************************************/

package org.eclipse.birt.chart.reportitem.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.birt.chart.device.IDisplayServer;
import org.eclipse.birt.chart.exception.ChartException;
import org.eclipse.birt.chart.log.ILogger;
import org.eclipse.birt.chart.log.Logger;
import org.eclipse.birt.chart.model.Chart;
import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.Bounds;
import org.eclipse.birt.chart.reportitem.ChartReportItemImpl;
import org.eclipse.birt.chart.reportitem.ChartReportItemUtil;
import org.eclipse.birt.chart.reportitem.ChartReportStyleProcessor;
import org.eclipse.birt.chart.reportitem.ChartXTabUtil;
import org.eclipse.birt.chart.reportitem.ui.dialogs.ChartExpressionProvider;
import org.eclipse.birt.chart.reportitem.ui.i18n.Messages;
import org.eclipse.birt.chart.ui.swt.interfaces.IChartDataSheet;
import org.eclipse.birt.chart.ui.swt.interfaces.IDataServiceProvider;
import org.eclipse.birt.chart.ui.swt.interfaces.IUIServiceProvider;
import org.eclipse.birt.chart.ui.swt.wizard.ApplyButtonHandler;
import org.eclipse.birt.chart.ui.swt.wizard.ChartWizard;
import org.eclipse.birt.chart.ui.swt.wizard.ChartWizardContext;
import org.eclipse.birt.chart.ui.util.ChartHelpContextIds;
import org.eclipse.birt.chart.ui.util.ChartUIConstants;
import org.eclipse.birt.chart.ui.util.ChartUIUtil;
import org.eclipse.birt.report.designer.ui.dialogs.ExpressionBuilder;
import org.eclipse.birt.report.designer.ui.dialogs.ExpressionProvider;
import org.eclipse.birt.report.designer.ui.dialogs.HyperlinkBuilder;
import org.eclipse.birt.report.designer.ui.extensions.ReportItemBuilderUI;
import org.eclipse.birt.report.designer.util.DEUtil;
import org.eclipse.birt.report.model.api.CommandStack;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;
import org.eclipse.birt.report.model.api.extension.IReportItem;
import org.eclipse.birt.report.model.api.util.DimensionUtil;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.ibm.icu.text.NumberFormat;

/**
 * ChartReportItemBuilderImpl
 */
public class ChartReportItemBuilderImpl extends ReportItemBuilderUI implements
		IUIServiceProvider
{

	protected static int iInstanceCount = 0;

	protected transient ExtendedItemHandle extendedHandle = null;

	private transient String taskId = null;

	protected static ILogger logger = Logger.getLogger( "org.eclipse.birt.chart.reportitem/trace" ); //$NON-NLS-1$

	/**
	 * The constructor.
	 */
	public ChartReportItemBuilderImpl( )
	{
		super( );
	}

	/**
	 * Open the chart with specified task
	 * 
	 * @param taskId
	 *            specified task to open
	 */
	public ChartReportItemBuilderImpl( String taskId )
	{
		super( );
		this.taskId = taskId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.extensions.IReportItemBuilderUI#open(org.eclipse.birt.report.model.api.ExtendedItemHandle)
	 */
	public int open( final ExtendedItemHandle eih )
	{
		if ( iInstanceCount > 0 ) // LIMIT TO ONE INSTANCE
		{
			return Window.CANCEL;
		}
		iInstanceCount++;

		if ( ChartXTabUtil.isAxisChart( eih ) )
		{
			// If this handle hosts another chart, use the host chart directly
			DesignElementHandle hostChart = eih.getElementProperty( ChartReportItemUtil.PROPERTY_HOST_CHART );
			this.extendedHandle = (ExtendedItemHandle) hostChart;
		}
		else
		{
			// Set the ExtendedItemHandle instance (for use by the Chart Builder
			// UI
			this.extendedHandle = eih;
		}

		try
		{
			IReportItem item = null;
			try
			{
				item = extendedHandle.getReportItem( );
				if ( item == null )
				{
					extendedHandle.loadExtendedElement( );
					item = extendedHandle.getReportItem( );
				}
			}
			catch ( ExtendedElementException exception )
			{
				logger.log( exception );
			}
			if ( item == null )
			{
				logger.log( ILogger.ERROR,
						Messages.getString( "ChartReportItemBuilderImpl.log.UnableToLocate" ) ); //$NON-NLS-1$
				return Window.CANCEL;
			}

			final CommandStack commandStack = extendedHandle.getRoot( )
					.getCommandStack( );
			final String TRANS_NAME = org.eclipse.birt.chart.reportitem.i18n.Messages.getString( "ChartElementCommandImpl.editChart" ); //$NON-NLS-1$
			commandStack.startTrans( TRANS_NAME );

			final ChartReportItemImpl crii = ( (ChartReportItemImpl) item );
			final Chart cm = (Chart) crii.getProperty( ChartReportItemUtil.PROPERTY_CHART );
			final Chart cmClone = ( cm == null ) ? null
					: (Chart) EcoreUtil.copy( cm );
			// This array is for storing the latest chart data before pressing
			// apply button
			final Object[] applyData = new Object[2];

			// Use workbench shell to open the dialog
			Shell parentShell = null;
			if ( PlatformUI.isWorkbenchRunning( ) )
			{
				parentShell = PlatformUI.getWorkbench( )
						.getDisplay( )
						.getActiveShell( );
			}
			final ChartWizard chartBuilder = new ChartWizard( parentShell );
			ReportDataServiceProvider dataProvider = new ReportDataServiceProvider( extendedHandle );
			IChartDataSheet dataSheet = new StandardChartDataSheet( extendedHandle,
					dataProvider );
			final ChartWizardContext context = new ChartWizardContext( cmClone,
					this,
					dataProvider,
					dataSheet );
			dataProvider.setWizardContext( context );
			if ( dataProvider.checkState( IDataServiceProvider.PART_CHART ) )
			{
				// Disable some UI sections for xtab case
				context.setEnabled( ChartUIConstants.SUBTASK_AXIS, false );
				context.setEnabled( ChartUIConstants.SUBTASK_AXIS_X, false );
				context.setEnabled( ChartUIConstants.SUBTASK_AXIS_Y, false );
				context.setEnabled( ChartUIConstants.SUBTASK_AXIS_Z, false );
				context.setEnabled( ChartUIConstants.SUBTASK_LEGEND, false );
				context.setEnabled( ChartUIConstants.SUBTASK_TITLE, false );

				// Disable some chart types
				context.setEnabled( ChartUIConstants.TYPE_PIE, false );
				context.setEnabled( ChartUIConstants.TYPE_METER, false );
				context.setEnabled( ChartUIConstants.TYPE_STOCK, false );
				context.setEnabled( ChartUIConstants.TYPE_BUBBLE, false );
				context.setEnabled( ChartUIConstants.TYPE_DIFFERENCE, false );
				context.setEnabled( ChartUIConstants.TYPE_GANTT, false );
			}
			chartBuilder.addCustomButton( new ApplyButtonHandler( chartBuilder ) {

				public void run( )
				{
					super.run( );
					// Save the data when applying
					applyData[0] = EcoreUtil.copy( context.getModel( ) );
					applyData[1] = context.getOutputFormat( );

					commandStack.commit( );
					commandStack.startTrans( TRANS_NAME );
				}
			} );

			// Set direction from model to chart
			context.setRtL( extendedHandle.isDirectionRTL( ) );
			context.setTextRtL( DesignChoiceConstants.BIDI_DIRECTION_RTL.equals( extendedHandle.getPrivateStyle( )
					.getTextDirection( ) ) );
			
			context.setResourceFinder( crii );
			context.setExternalizer( crii );
			
			Object of = extendedHandle.getProperty( ChartReportItemUtil.PROPERTY_OUTPUT );
			if ( of instanceof String )
			{
				// GIF is deprecated in favor of PNG. Automatically update
				// model
				if ( of.equals( "GIF" ) ) //$NON-NLS-1$
				{
					context.setOutputFormat( "PNG" ); //$NON-NLS-1$
				}
				else
					context.setOutputFormat( (String) of );
			}
			context.setExtendedItem( extendedHandle );
			context.setProcessor( new ChartReportStyleProcessor( extendedHandle,
					false ) );
			ChartWizardContext contextResult = (ChartWizardContext) chartBuilder.open( null,
					taskId,
					context );
			if ( contextResult != null && contextResult.getModel( ) != null )
			{
				// Pressing Finish
				updateModel( extendedHandle,
						chartBuilder,
						crii,
						cm,
						contextResult.getModel( ),
						contextResult.getOutputFormat( ) );
				if ( dataProvider.isPartChart( ) )
				{
					ChartXTabUIUtil.updateXTabForAxis( ChartXTabUtil.getXtabContainerCell( extendedHandle ),
							extendedHandle,
							ChartXTabUIUtil.isTransposedChartWithAxes( cm ),
							(ChartWithAxes) contextResult.getModel( ) );
				}
				commandStack.commit( );
				return Window.OK;
			}
			else if ( applyData[0] != null )
			{
				// Pressing Cancel but Apply was pressed before, so revert to
				// the point pressing Apply
				commandStack.rollback( );
				updateModel( extendedHandle,
						chartBuilder,
						crii,
						cm,
						(Chart) applyData[0],
						(String) applyData[1] );
				if ( dataProvider.isPartChart( ) )
				{
					commandStack.startTrans( TRANS_NAME );
					ChartXTabUIUtil.updateXTabForAxis( ChartXTabUtil.getXtabContainerCell( extendedHandle ),
							extendedHandle,
							ChartXTabUIUtil.isTransposedChartWithAxes( cm ),
							(ChartWithAxes) applyData[0] );
					commandStack.commit( );
				}
				return Window.OK;
			}
			commandStack.rollback( );
			return Window.CANCEL;
		}
		catch ( Exception e )
		{
			throw new RuntimeException( e );
		}
		finally
		{
			iInstanceCount--;
			// Reset the ExtendedItemHandle instance since it is no
			// longer needed
			this.extendedHandle = null;
		}
	}

	private void updateModel( ExtendedItemHandle eih, ChartWizard chartBuilder,
			ChartReportItemImpl crii, Chart cmOld, Chart cmNew,
			String outputFormat )
	{
		try
		{
			// update the output format property information.
			eih.setProperty( ChartReportItemUtil.PROPERTY_OUTPUT, outputFormat );

			// TODO: Added till the model team sorts out pass-through
			// for setProperty
			crii.executeSetModelCommand( eih, cmOld, cmNew );

			// Resizes chart with a default value when the size is zero or null
			if ( cmNew.getBlock( ).getBounds( ) == null
					|| cmNew.getBlock( ).getBounds( ).getWidth( ) == 0
					|| cmNew.getBlock( ).getBounds( ).getHeight( ) == 0 )
			{
				cmNew.getBlock( )
						.setBounds( ChartReportItemUtil.createDefaultChartBounds( eih,
								cmNew ) );
			}

			final Bounds bo = cmNew.getBlock( ).getBounds( );

			// Modified to fix Bugzilla #99331
			NumberFormat nf = ChartUIUtil.getDefaultNumberFormatInstance( );

			if ( eih.getWidth( ).getStringValue( ) == null )
			{
				eih.setWidth( nf.format( bo.getWidth( ) ) + "pt" ); //$NON-NLS-1$
			}
			if ( eih.getHeight( ).getStringValue( ) == null )
			{
				eih.setHeight( nf.format( bo.getHeight( ) ) + "pt" ); //$NON-NLS-1$
			}
		}
		catch ( SemanticException smx )
		{
			logger.log( smx );
		}

		if ( crii.getDesignerRepresentation( ) != null )
		{
			( (DesignerRepresentation) crii.getDesignerRepresentation( ) ).setDirty( true );
		}
	}

	/**
	 * @deprecated
	 */
	public String invoke( String sExpression, Object oContext, String sTitle )
	{
		final ExpressionBuilder eb = new ExpressionBuilder( sExpression );
		eb.setExpressionProvier( new ExpressionProvider( (ExtendedItemHandle) oContext ) );
		if ( sTitle != null )
		{
			eb.setDialogTitle( eb.getDialogTitle( ) + " - " + sTitle ); //$NON-NLS-1$
		}
		if ( eb.open( ) == Window.OK )
		{
			sExpression = eb.getResult( );
		}
		return sExpression;
	}

	/**
	 * @deprecated
	 */
	public String invoke( String sExpression, Object oContext, String sTitle,
			boolean isChartProvider )
	{
		final ExpressionBuilder eb = new ExpressionBuilder( sExpression );
		eb.setExpressionProvier( new ChartExpressionProvider( ) );
		if ( sTitle != null )
		{
			eb.setDialogTitle( eb.getDialogTitle( ) + " - " + sTitle ); //$NON-NLS-1$
		}
		if ( eb.open( ) == Window.OK )
		{
			sExpression = eb.getResult( );
		}
		return sExpression;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.chart.ui.swt.interfaces.IUIServiceProvider#validate(org.eclipse.birt.chart.model.Chart,
	 *      java.lang.Object)
	 */
	public final String[] validate( Chart cm, Object oContext )
	{
		final ArrayList alProblems = new ArrayList( 4 );

		// CHECK FOR UNBOUND DATASET
		final ExtendedItemHandle eih = (ExtendedItemHandle) oContext;
		if ( DEUtil.getDataSetList( eih ).size( ) == 0
				&& ChartXTabUtil.getBindingCube( eih ) == null )
		{
			alProblems.add( Messages.getString( "ChartReportItemBuilderImpl.problem.hasNotBeenFound" ) ); //$NON-NLS-1$
		}

		// CHECK FOR UNDEFINED SERIES QUERIES (DO NOT NEED THE RUNTIME CONTEXT)
		final QueryUIHelper.SeriesQueries[] qsqa = new QueryUIHelper( ).getSeriesQueryDefinitions( cm );
		Collection co;
		for ( int i = 0; i < qsqa.length; i++ )
		{
			co = qsqa[i].validate( );
			if ( co != null )
			{
				alProblems.addAll( co );
			}
		}

		return (String[]) alProblems.toArray( new String[alProblems.size( )] );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.chart.ui.swt.interfaces.IUIServiceProvider#getRegisteredKeys()
	 */
	public final List getRegisteredKeys( )
	{
		return extendedHandle.getModuleHandle( ).getMessageKeys( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.chart.ui.swt.interfaces.IUIServiceProvider#getValue(java.lang.String)
	 */
	public final String getValue( String sKey )
	{
		String value = extendedHandle.getModuleHandle( ).getMessage( sKey );
		if ( value == null || value.equals( "" ) ) //$NON-NLS-1$
		{
			return sKey;
		}
		return value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.chart.ui.swt.interfaces.IUIServiceProvider#getConvertedValue(double,
	 *      java.lang.String, java.lang.String)
	 */
	public final double getConvertedValue( double dOriginalValue,
			String sFromUnits, String sToUnits )
	{
		if ( sFromUnits == null || sToUnits == null )
		{
			return dOriginalValue;
		}
		double dResult = -1d;

		// CONVERT FROM PIXELS
		final IDisplayServer ids = ChartUIUtil.getDisplayServer( );
		if ( sFromUnits.equalsIgnoreCase( "pixels" ) ) //$NON-NLS-1$
		{
			dOriginalValue = ( dOriginalValue * 72d ) / ids.getDpiResolution( );
		}

		// Convert to target units - Will convert to Points if target is Points,
		// Pixels or Unknown
		dResult = ( DimensionUtil.convertTo( dOriginalValue,
				getBIRTUnitsFor( sFromUnits ),
				getBIRTUnitsFor( sToUnits ) ) ).getMeasure( );

		// Special handling to convert TO Pixels
		if ( sToUnits.equalsIgnoreCase( "pixels" ) ) //$NON-NLS-1$
		{
			dResult = ( ids.getDpiResolution( ) * dResult ) / 72d;
		}
		return dResult;
	}

	/**
	 * @param sUnits
	 */
	private static String getBIRTUnitsFor( String sUnits )
	{
		if ( sUnits.equalsIgnoreCase( "inches" ) ) //$NON-NLS-1$
		{
			return DesignChoiceConstants.UNITS_IN;
		}
		else if ( sUnits.equalsIgnoreCase( "centimeters" ) ) //$NON-NLS-1$
		{
			return DesignChoiceConstants.UNITS_CM;
		}
		else
		{
			return DesignChoiceConstants.UNITS_PT;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.chart.ui.swt.interfaces.IUIServiceProvider#invoke(int,
	 *      java.lang.String, java.lang.Object, java.lang.String)
	 */
	public String invoke( int command, String value, final Object context,
			String sTitle ) throws ChartException
	{
		final ExpressionProvider ep = new ChartExpressionProvider( (ExtendedItemHandle) context,
				getExpressionBuilderStyle( command ) );
		Shell shell = null;

		switch ( command )
		{
			case COMMAND_HYPERLINK :
			case COMMAND_HYPERLINK_DATAPOINTS :
				shell = new Shell( Display.getDefault( ), SWT.DIALOG_TRIM
						| SWT.RESIZE
						| SWT.APPLICATION_MODAL );
				ChartUIUtil.bindHelp( shell,
						ChartHelpContextIds.DIALOG_EDIT_URL );
				HyperlinkBuilder hb = new HyperlinkBuilder( shell, true ) {

					protected void configureExpressionBuilder(
							ExpressionBuilder builder )
					{
						builder.setExpressionProvier( ep );
					}

				};
				try
				{
					hb.setInputString( value, extendedHandle );
					if ( sTitle != null )
					{
						hb.setTitle( hb.getTitle( ) + " - " + sTitle ); //$NON-NLS-1$
					}
					if ( hb.open( ) == Window.OK )
					{
						value = hb.getResultString( );
					}
				}
				catch ( Exception e )
				{
					throw new ChartException( ChartReportItemUIActivator.ID,
							ChartException.UNDEFINED_VALUE,
							e );
				}
				break;

			case COMMAND_EXPRESSION_CHART_DATAPOINTS :
			case COMMAND_EXPRESSION_DATA_BINDINGS :
			case COMMAND_EXPRESSION_TRIGGERS_SIMPLE :
			case COMMAND_EXPRESSION_SCRIPT_DATAPOINTS :
			case COMMAND_EXPRESSION_TOOLTIPS_DATAPOINTS :
				shell = new Shell( Display.getDefault( ), SWT.DIALOG_TRIM
						| SWT.RESIZE
						| SWT.APPLICATION_MODAL );
				ChartUIUtil.bindHelp( shell,
						ChartHelpContextIds.DIALOG_EXPRESSION_BUILDER );
				ExpressionBuilder eb = new ExpressionBuilder( shell, value );
				eb.setExpressionProvier( ep );
				if ( sTitle != null )
				{
					eb.setDialogTitle( eb.getDialogTitle( ) + " - " + sTitle ); //$NON-NLS-1$
				}
				if ( eb.open( ) == Window.OK )
				{
					value = eb.getResult( );
				}
		}

		return value;
	}

	public boolean isInvokingSupported( )
	{
		return true;
	}

	public boolean isEclipseModeSupported( )
	{
		return true;
	}

	/**
	 * Returns the categories list in BIRT chart expression builder
	 * 
	 * @param builderCommand
	 * @return category style
	 */
	private int getExpressionBuilderStyle( int builderCommand )
	{
		if ( builderCommand == COMMAND_EXPRESSION_DATA_BINDINGS )
		{
			return ChartExpressionProvider.CATEGORY_WITH_BIRT_VARIABLES
					| ChartExpressionProvider.CATEGORY_WITH_COLUMN_BINDINGS
					| ChartExpressionProvider.CATEGORY_WITH_REPORT_PARAMS;
		}
		else if ( builderCommand == COMMAND_EXPRESSION_CHART_DATAPOINTS )
		{
			return ChartExpressionProvider.CATEGORY_WITH_CHART_VARIABLES;
		}
		else if ( builderCommand == COMMAND_EXPRESSION_SCRIPT_DATAPOINTS )
		{
			// Script doesn't support column binding expression.
			return ChartExpressionProvider.CATEGORY_WITH_CHART_VARIABLES
					| ChartExpressionProvider.CATEGORY_WITH_REPORT_PARAMS
					| ChartExpressionProvider.CATEGORY_WITH_JAVASCRIPT;
		}
		else if ( builderCommand == COMMAND_EXPRESSION_TRIGGERS_SIMPLE )
		{
			// Bugzilla#202386: Tooltips never support chart
			// variables. Use COMMAND_EXPRESSION_TRIGGERS_SIMPLE for un-dp
			return ChartExpressionProvider.CATEGORY_WITH_REPORT_PARAMS
					| ChartExpressionProvider.CATEGORY_WITH_JAVASCRIPT;
		}
		else if ( builderCommand == COMMAND_EXPRESSION_TOOLTIPS_DATAPOINTS )
		{
			// Bugzilla#202386: Tooltips never support chart
			// variables. Use COMMAND_EXPRESSION_TOOLTIPS_DATAPOINTS for dp
			return ChartExpressionProvider.CATEGORY_WITH_REPORT_PARAMS
					| ChartExpressionProvider.CATEGORY_WITH_COLUMN_BINDINGS
					| ChartExpressionProvider.CATEGORY_WITH_JAVASCRIPT;
		}
		else if ( builderCommand == COMMAND_HYPERLINK )
		{
			return ChartExpressionProvider.CATEGORY_WITH_BIRT_VARIABLES
					| ChartExpressionProvider.CATEGORY_WITH_REPORT_PARAMS;
		}
		else if ( builderCommand == COMMAND_HYPERLINK_DATAPOINTS )
		{
			return ChartExpressionProvider.CATEGORY_WITH_BIRT_VARIABLES
					| ChartExpressionProvider.CATEGORY_WITH_COLUMN_BINDINGS
					| ChartExpressionProvider.CATEGORY_WITH_REPORT_PARAMS;
		}
		return ChartExpressionProvider.CATEGORY_BASE;
	}
}