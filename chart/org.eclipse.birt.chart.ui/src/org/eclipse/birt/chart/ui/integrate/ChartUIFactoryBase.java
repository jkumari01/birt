/*******************************************************************************
 * Copyright (c) 2010 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.chart.ui.integrate;

import org.eclipse.birt.chart.ui.swt.interfaces.IChartUIFactory;
import org.eclipse.birt.chart.ui.swt.interfaces.IChartUIHelper;
import org.eclipse.birt.chart.util.TriggerSupportMatrix;

/**
 * Default implementation or base class of UI factory interface.
 */

public class ChartUIFactoryBase implements IChartUIFactory
{

	public IChartUIHelper createUIHelper( )
	{
		return new ChartUIHelperBase( );
	}

	public TriggerSupportMatrix createSupportMatrix( String outputFormat,
			int iType )
	{
		return new TriggerSupportMatrix( outputFormat, iType );
	}

}
