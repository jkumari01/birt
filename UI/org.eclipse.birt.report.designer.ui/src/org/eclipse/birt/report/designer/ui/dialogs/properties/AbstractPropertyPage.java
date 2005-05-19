/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Actuate Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.birt.report.designer.ui.dialogs.properties;

import org.eclipse.swt.graphics.Image;

/**
 * TODO: Please document
 * 
 * @version $Revision: 1.2 $ $Date: 2005/02/21 23:45:04 $
 */

public abstract class AbstractPropertyPage implements IPropertyPage
{

	private transient IPropertyPageContainer container = null;

	private transient String name = null;

	private transient Image image = null;
    

	/**
	 *  
	 */
	public AbstractPropertyPage( )
	{
		super( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.IPropertyPage#setContainer(org.eclipse.birt.report.designer.ui.IPropertyPageContainer)
	 */
	public void setContainer( IPropertyPageContainer parentContainer )
	{
		this.container = parentContainer;
	}

	public IPropertyPageContainer getContainer( )
	{
		return container;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.IPropertyPage#getName()
	 */
	public String getName( )
	{
		return name;
	}

	public void setName( String name )
	{
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.IPropertyPage#getImage()
	 */
	public Image getImage( )
	{
		return image;
	}

	public void setImage( Image image )
	{
		this.image = image;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.IPropertyPage#canLeave()
	 */
	public boolean canLeave( )
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.IPropertyPage#performOk()
	 */
	public boolean performOk( )
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.IPropertyPage#performCancel()
	 */
	public boolean performCancel( )
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.report.designer.ui.IPropertyPage#performHelp()
	 */
	public void performHelp( )
	{
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.birt.report.designer.ui.dialogs.properties.IPropertyPage#getToolTip()
	 */
	public String getToolTip( )
	{
		return "";  //$NON-NLS-1$
	}	
}