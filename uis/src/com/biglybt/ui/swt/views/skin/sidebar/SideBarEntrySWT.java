/*
 * Created on Aug 13, 2008
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */

package com.biglybt.ui.swt.views.skin.sidebar;

import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.config.ParameterListener;
import com.biglybt.core.util.AERunnable;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.Debug;
import com.biglybt.core.util.SystemTime;
import com.biglybt.ui.common.updater.UIUpdatable;
import com.biglybt.ui.common.updater.UIUpdater;
import com.biglybt.ui.common.viewtitleinfo.ViewTitleInfo;
import com.biglybt.ui.mdi.MdiEntry;
import com.biglybt.ui.mdi.MdiEntryVitalityImage;
import com.biglybt.ui.mdi.MultipleDocumentInterface;
import com.biglybt.ui.skin.SkinConstants;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.debug.ObfuscateImage;
import com.biglybt.ui.swt.debug.ObfuscateTab;
import com.biglybt.ui.swt.debug.UIDebugGenerator;
import com.biglybt.ui.swt.imageloader.ImageLoader;
import com.biglybt.ui.swt.mainwindow.Colors;
import com.biglybt.ui.swt.mainwindow.SWTThread;
import com.biglybt.ui.swt.mdi.BaseMdiEntry;
import com.biglybt.ui.swt.pif.UISWTView;
import com.biglybt.ui.swt.pif.UISWTViewEvent;
import com.biglybt.ui.swt.pif.UISWTViewEventListener;
import com.biglybt.ui.swt.pif.UISWTViewEventListenerEx;
import com.biglybt.ui.swt.pifimpl.UISWTViewCoreEventListenerEx;
import com.biglybt.ui.swt.pifimpl.UISWTViewImpl;
import com.biglybt.ui.swt.shells.GCStringPrinter;
import com.biglybt.ui.swt.skin.SWTSkin;
import com.biglybt.ui.swt.skin.SWTSkinObject;
import com.biglybt.ui.swt.skin.SWTSkinObjectContainer;
import com.biglybt.ui.swt.skin.SWTSkinObjectListener;
import com.biglybt.ui.swt.skin.SWTSkinProperties;
import com.biglybt.ui.swt.uiupdater.UIUpdaterSWT;
import com.biglybt.ui.swt.utils.ColorCache;
import com.biglybt.ui.swt.utils.SWTRunnable;
import com.biglybt.ui.swt.views.IViewRequiresPeriodicUpdates;
import com.biglybt.ui.swt.views.skin.InfoBarUtil;
import com.biglybt.ui.swt.views.stats.StatsView;
import com.biglybt.util.MapUtils;

/**
 * @author TuxPaper
 * @created Aug 13, 2008
 *
 */
public class SideBarEntrySWT
	extends BaseMdiEntry
	implements DisposeListener
{
	private static final boolean PAINT_BG = !Constants.isUnix;

	private static final boolean DO_OUR_OWN_TREE_INDENT = true;
		
	private static final int		EXPANDO_WIDTH				= 12;
	private static final int		EXPANDO_INDENT				= 10;

	private static boolean DO_EXPANDO_INDENT;
	private static boolean COMPACT_SIDEBAR;

	private static int				EXPANDO_LEFT_INDENT;
	private static int				EXPANDO_INDENT_INITIAL;
	
	private static boolean			IMAGELEFT_HIDDEN;
	
	static{
		COConfigurationManager.addAndFireParameterListeners(
				new String[]{
					"Side Bar Indent Expanders",
					"Side Bar Compact View",
					"Side Bar Hide Left Icon",
				},
				new ParameterListener(){
					
					@Override
					public void parameterChanged(String name ){
						
						DO_EXPANDO_INDENT = COConfigurationManager.getBooleanParameter( "Side Bar Indent Expanders" );
						COMPACT_SIDEBAR = COConfigurationManager.getBooleanParameter( "Side Bar Compact View" );
						
						EXPANDO_LEFT_INDENT 	= COMPACT_SIDEBAR?4:10;
						EXPANDO_INDENT_INITIAL	= EXPANDO_WIDTH + EXPANDO_LEFT_INDENT;
						
						IMAGELEFT_HIDDEN = COConfigurationManager.getBooleanParameter( "Side Bar Hide Left Icon" );
					}
				});
	}
	
	private static final int SIDEBAR_SPACING = 2;

	private int IMAGELEFT_SIZE = 20;

	private int IMAGELEFT_GAP = 5;

	private static final boolean ALWAYS_IMAGE_GAP = true;

	private static int CLOSE_IMAGE_POSITION = 0;
	
	static{
		
		COConfigurationManager.addAndFireParameterListener(
			"Side Bar Close Position",
			new ParameterListener(){
				
				@Override
				public void parameterChanged(String name ){
					CLOSE_IMAGE_POSITION = COConfigurationManager.getIntParameter( name );
				}
			});
	}
	/*
	private static final String[] default_indicator_colors = {
		"#000000",
		"#000000",
		"#166688",
		"#1c2056"
	};
	*/

	private static final String SO_ID_ENTRY_WRAPPER = "mdi.content.item";

	private static final String SO_ID_TOOLBAR = "mdientry.toolbar.full";

	private static long uniqueNumber = 0;

	private TreeItem swtItem;

	@SuppressWarnings("unchecked")
	private List<SideBarVitalityImageSWT> listVitalityImages = Collections.EMPTY_LIST;

	private final SideBar sidebar;

	private int maxIndicatorWidth;

	private Image imgClose;

	private Image imgCloseSelected;

	private Color bg;

	private Color fg;

	private Color bgSel;

	private Color fgSel;

	//private Color colorFocus;

	private boolean showonSWTItemSet;

	private SWTSkinObjectContainer soParent;

	private boolean buildonSWTItemSet;

	private boolean selectable = true;

	private boolean neverPainted = true;

	private long 	attention_start = -1;
	private boolean	attention_flash_on;

	private Boolean	closeWasUserInitiated;

	public SideBarEntrySWT(SideBar sidebar, SWTSkin _skin, String id,
			String parentViewID) {
		super(sidebar, id, parentViewID);
		this.skin = _skin;

		if (id == null) {
			logID = "null";
		} else {
			int i = id.indexOf('_');
			if (i > 0) {
				logID = id.substring(0, i);
			} else {
				logID = id;
			}
		}

		this.sidebar = sidebar;

		updateColors();
	}

	protected void updateColors() {
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				SWTSkinProperties skinProperties = skin.getSkinProperties();
				bg = skinProperties.getColor("color.sidebar.bg");
				fg = skinProperties.getColor("color.sidebar."
						+ (isSelectable() ? "text" : "header"));
				bgSel = skinProperties.getColor("color.sidebar.selected.bg");
				fgSel = skinProperties.getColor("color.sidebar.selected.fg");
				//colorFocus = skinProperties.getColor("color.sidebar.focus");
			}
		});
	}

	public TreeItem getTreeItem() {
		return swtItem;
	}

	public void setTreeItem(TreeItem treeItem) {
		if (swtItem != null && treeItem != null) {
			Debug.out("Warning: Sidebar " + id + " already has a treeitem");
			return;
		}
		this.swtItem = treeItem;

		if (treeItem != null) {
			setDisposed(false);

			ImageLoader imageLoader = ImageLoader.getInstance();
			imgClose = imageLoader.getImage("image.sidebar.closeitem");
			imgCloseSelected = imageLoader.getImage("image.sidebar.closeitem-selected");

			treeItem.addDisposeListener(this);

			treeItem.getParent().addTreeListener(new TreeListener() {
				@Override
				public void treeExpanded(TreeEvent e) {
					if (e.item == swtItem) {
						SideBarEntrySWT.super.setExpanded(true);
					}
				}

				@Override
				public void treeCollapsed(TreeEvent e) {
					if (e.item == swtItem) {
						SideBarEntrySWT.super.setExpanded(false);
					}
				}
			});

			// Some/All OSes will auto-set treeitem's expanded flag to false if there
			// is no children.  To workaround, we store expanded state internally and
			// set parent to expanded when a child is added
			TreeItem parentItem = treeItem.getParentItem();
			if (parentItem != null) {
				MdiEntry parentEntry = (MdiEntry) parentItem.getData("MdiEntry");
				if (parentEntry.isExpanded()) {
					parentItem.setExpanded(true);
				}
			}

			setExpanded(isExpanded());
		}
		if (buildonSWTItemSet) {
			build();
		}
		if (showonSWTItemSet) {
			show();
		}
	}

	// @see com.biglybt.pif.ui.sidebar.SideBarEntry#addVitalityImage(java.lang.String)
	@Override
	public MdiEntryVitalityImage addVitalityImage(String imageID) {
		synchronized (this) {
		SideBarVitalityImageSWT vitalityImage = new SideBarVitalityImageSWT(this,
				imageID);
		if (listVitalityImages == Collections.EMPTY_LIST) {
			listVitalityImages = new ArrayList<>(1);
		}
		listVitalityImages.add(vitalityImage);
		return vitalityImage;
	}
	}

	@Override
	public MdiEntryVitalityImage[] getVitalityImages() {
		synchronized (this) {
			return listVitalityImages.toArray(new MdiEntryVitalityImage[0]);
		}
	}

	public MdiEntryVitalityImage getVitalityImage(int hitX, int hitY) {
		MdiEntryVitalityImage[] vitalityImages = getVitalityImages();
		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
			if (!vitalityImage.isVisible()) {
				continue;
			}
			Rectangle hitArea = vitalityImage.getHitArea();
			if (hitArea != null && hitArea.contains(hitX, hitY)) {
				return vitalityImage;
			}
		}
		return null;
	}

	@Override
	public void
	requestAttention()
	{
		attention_start = SystemTime.getMonotonousTime();

		sidebar.requestAttention( this );
	}

	protected boolean
	attentionUpdate(
		int	ticks )
	{
		if ( 	attention_start == -1 ||
				SystemTime.getMonotonousTime() - attention_start > SideBar.SIDEBAR_ATTENTION_DURATION ){

			attention_start = -1;

			return( false );
		}

		attention_flash_on = ticks%2==0;

		return( true );
	}

	/* (non-Javadoc)
	 * @see MdiEntry#redraw()
	 */
	boolean isRedrawQueued = false;

	private InfoBarUtil toolBarInfoBar;
	@Override
	public void redraw() {
		if (neverPainted) {
			return;
		}
		synchronized (this) {
			if (isRedrawQueued) {
				return;
			}
			isRedrawQueued = true;
		}

		//System.out.println("redraw " + Thread.currentThread().getName() + ":" + getId() + " via " + Debug.getCompressedStackTrace());

		Utils.execSWTThreadLater(0, new SWTRunnable() {
			@Override
			public void runWithDisplay(Display display) 
			{
				synchronized (SideBarEntrySWT.this) {
					isRedrawQueued = false;
				}
				
				if (swtItem == null || swtItem.isDisposed()) {
					return;
				}
				Tree tree = swtItem.getParent();
				if (!tree.isVisible()) {
					return;
				}
				if (Utils.isGTK3) {
					// parent.clear crashes java, so call item's clear
					//parent.clear(parent.indexOf(treeItem), true);
					try {
						Method m = swtItem.getClass().getDeclaredMethod("clear");
						m.setAccessible(true);
						m.invoke(swtItem);
					} catch (Throwable e) {
					}
				} else {

					try {
						Rectangle bounds = swtItem.getBounds();
						Rectangle treeBounds = tree.getBounds();
						tree.redraw(0, bounds.y, treeBounds.width, bounds.height, true);
					} catch (NullPointerException npe) {
						// ignore NPE. OSX seems to be spewing this when the tree size is 0
						// or is invisible or something like that
					}
				}
				//tree.update();
			}
		});
	}

	protected Rectangle swt_getBounds() {
		if (swtItem == null || swtItem.isDisposed()) {
			return null;
		}
		try {
			Tree tree = swtItem.getParent();
			Rectangle bounds = swtItem.getBounds();
			Rectangle treeBounds = tree.getClientArea();
			return new Rectangle(0, bounds.y, treeBounds.width, bounds.height);
		} catch (NullPointerException e) {
			// On OSX, we get erroneous NPE here:
			//at org.eclipse.swt.widgets.Tree.sendMeasureItem(Tree.java:2443)
			//at org.eclipse.swt.widgets.Tree.cellSize(Tree.java:274)
			//at org.eclipse.swt.widgets.Display.windowProc(Display.java:4750)
			//at org.eclipse.swt.internal.cocoa.OS.objc_msgSend_stret(Native Method)
			//at org.eclipse.swt.internal.cocoa.NSCell.cellSize(NSCell.java:34)
			//at org.eclipse.swt.widgets.TreeItem.getBounds(TreeItem.java:467)
			Debug.outNoStack("NPE @ " + Debug.getCompressedStackTrace(), true);
		} catch (Exception e) {
			Debug.out(e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see BaseMdiEntry#setExpanded(boolean)
	 */
	@Override
	public void setExpanded(final boolean expanded) {
		super.setExpanded(expanded);
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (swtItem != null && !isDisposed()) {
					swtItem.setExpanded(expanded);
				}
			}
		});
	}

	@Override
	public void expandTo() {
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (swtItem == null || isDisposed()) {
					return;
				}

				TreeItem item = swtItem.getParentItem();
				while (item != null) {
					item.setExpanded(true);
					// walk up and make sure parents are expanded
					item = item.getParentItem();
				}
			}
		});
	}

	public boolean 
	close(boolean force, boolean userInitiated ) {
		if (!super.close(force)) {
			return false;
		}
		
		closeWasUserInitiated = userInitiated;
		
		return( close( force ));
	}
		
	/* (non-Javadoc)
	 * @see BaseMdiEntry#close()
	 */
	@Override
	public boolean close(boolean force) {
		if (!super.close(force)) {
			return false;
		}

		// remove immediately from MDI because disposal is on a delay
		mdi.removeItem(SideBarEntrySWT.this);

		// dispose will trigger dispose listener, which removed it from BaseMDI
		Utils.execSWTThread(new SWTRunnable() {
			@Override
			public void runWithDisplay(Display display) {
				if (swtItem != null && !swtItem.isDisposed()) {
					try {
  					swtItem.setFont(null);
  					swtItem.dispose();
					} catch (Exception e) {
						// on OSX, SWT does some misguided exceptions on disposal of TreeItem
						// We occasionally get SWTException of "Widget is Disposed" or
						// "Argument not valid", as well as NPEs
						Debug.outNoStack(
								"Warning on SidebarEntry dispose: " + e.toString(), false);
					} finally {
  					swtItem = null;
					}
				}
			}
		});
		return true;
	}

	
	public SWTSkinObjectContainer
	buildStandAlone(
		SWTSkinObjectContainer		soParent )
	{
		return(
			buildStandAlone(
					soParent,
					getSkinRef(),
					skin,
					getParentID(),
					id,
					getDatasourceCore(),
					getControlType(),
					swtItem,
					getEventListener(),
					false ));
	}
	
	
	public static SWTSkinObjectContainer
	buildStandAlone(
		SWTSkinObjectContainer		soParent,
		String						skinRef,
		SWTSkin						skin,
		String						parentID,
		String						id,
		Object						datasource,
		int							controlType,
		TreeItem					swtItem,
		UISWTViewEventListener		original_event_listener,
		boolean						listener_is_new )
	{
		Control control = null;

		//SWTSkin skin = soParent.getSkin();

		Composite parent = soParent.getComposite();

		if (skinRef != null){

			Shell shell = parent.getShell();
			Cursor cursor = shell.getCursor();
			try {
				shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

				// wrap skinRef with a container that we control visibility of
				// (invisible by default)
				SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
						"MdiContents." + uniqueNumber++, SO_ID_ENTRY_WRAPPER,
						soParent, null);

				SWTSkinObject skinObject = skin.createSkinObject( id, skinRef, soContents, datasource );

				control = skinObject.getControl();
				control.setLayoutData(Utils.getFilledFormData());
				control.getParent().layout(true, true);

				soContents.setVisible( true );

				return( soContents );

			}finally{
				shell.setCursor(cursor);
			}
		}else {
			// XXX: This needs to be merged into BaseMDIEntry.initialize

			if ( 	( original_event_listener instanceof UISWTViewCoreEventListenerEx && ((UISWTViewCoreEventListenerEx)original_event_listener).isCloneable()) ||
					( original_event_listener instanceof UISWTViewEventListenerEx )){

				final UISWTViewImpl view = new UISWTViewImpl( id, parentID, true );

				final UISWTViewEventListener event_listener = original_event_listener instanceof UISWTViewEventListenerEx?((UISWTViewEventListenerEx)original_event_listener).getClone():((UISWTViewCoreEventListenerEx)original_event_listener).getClone();

				try{
					view.setEventListener( event_listener, false );

				}catch( Throwable e ){
					// shouldn't happen as we aren't asking for 'create' to occur which means it can't fail
					Debug.out( e );
				}

				view.setDatasource( datasource );

				try {
					SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
							"MdiIView." + uniqueNumber++, SO_ID_ENTRY_WRAPPER,
							soParent );

					parent.setBackgroundMode(SWT.INHERIT_NONE);

					final Composite viewComposite = soContents.getComposite();
					boolean doGridLayout = true;
					if ( controlType == CONTROLTYPE_SKINOBJECT) {
						doGridLayout = false;
					}
					//					viewComposite.setBackground(parent.getDisplay().getSystemColor(
					//							SWT.COLOR_WIDGET_BACKGROUND));
					//					viewComposite.setForeground(parent.getDisplay().getSystemColor(
					//							SWT.COLOR_WIDGET_FOREGROUND));
					if (doGridLayout) {
						GridLayout gridLayout = new GridLayout();
						gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
						viewComposite.setLayout(gridLayout);
						viewComposite.setLayoutData(Utils.getFilledFormData());
					}

					view.setPluginSkinObject(soContents);
					view.initialize(viewComposite);

						// without this some views get messed up layouts (chat view for example)
					
					viewComposite.setData( Utils.RELAYOUT_UP_STOP_HERE, true );

					soContents.addListener(
							new SWTSkinObjectListener(){
								
								@Override
								public Object eventOccured(SWTSkinObject skinObject, int eventType, Object params){
									if ( eventType == SWTSkinObjectListener.EVENT_OBFUSCATE ){
										Map data = new HashMap();
										data.put( "image", (Image)params );
										data.put( "obfuscateTitle",false );
										
										view.triggerEvent(UISWTViewEvent.TYPE_OBFUSCATE, data);
									}
									return null;
								}
							});
					
					if (PAINT_BG) {
						if ( swtItem != null ){
							swtItem.setText(view.getFullTitle());
						}
					}

					Composite iviewComposite = view.getComposite();
					control = iviewComposite;
					// force layout data of IView's composite to GridData, since we set
					// the parent to GridLayout (most plugins use grid, so we stick with
					// that instead of form)
					if (doGridLayout) {
						Object existingLayoutData = iviewComposite.getLayoutData();
						Object existingParentLayoutData = iviewComposite.getParent().getLayoutData();
						if (existingLayoutData == null
								|| !(existingLayoutData instanceof GridData)
								&& (existingParentLayoutData instanceof GridLayout)) {
							GridData gridData = new GridData(GridData.FILL_BOTH);
							iviewComposite.setLayoutData(gridData);
						}
					}

					parent.layout(true, true);

					final UIUpdater updater = UIUpdaterSWT.getInstance();
					if (updater != null) {
						updater.addUpdater(new UIUpdatable() {
							@Override
							public void updateUI() {
								if (viewComposite.isDisposed()) {
									updater.removeUpdater(this);
								} else {
									view.triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
								}
							}

							@Override
							public String getUpdateUIName() {
								return ("popout");
							}
						});
						
						if ( event_listener instanceof IViewRequiresPeriodicUpdates ){
							
							updater.addPeriodicUpdater(
								new UIUpdatable() {

									@Override
									public void updateUI() {
										if (viewComposite.isDisposed()) {
											updater.removePeriodicUpdater(this);
										} else {
											event_listener.eventOccurred(
												new UISWTViewEvent() {										
													@Override
													public UISWTView getView() {
														return null;
													}
													
													@Override
													public int getType() {
														return( StatsView.EVENT_PERIODIC_UPDATE );
													}										
													@Override
													public Object getData() {
													
														return null;
													}
												});
										}
									}

									@Override
									public String getUpdateUIName() {
										return ("popout");
									}
								});						
							}
					}

					soContents.setVisible( true );

					view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);

					iviewComposite.addDisposeListener(
						new DisposeListener(){
							
							@Override
							public void widgetDisposed(DisposeEvent arg0){
								view.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
							}
						});
					
					return( soContents );

				} catch (Throwable e) {

					Debug.out(e);
				}
			}
		}

		return( null );
	}

	@Override
	public void build() {
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				swt_build();
				SideBarEntrySWT.super.build();
			}
		});
	}

	public boolean swt_build() {
		if (swtItem == null) {
			buildonSWTItemSet = true;
			return true;
		}
		buildonSWTItemSet = false;

		if (getSkinObject() == null) {
			Control control = null;

			Composite parent = soParent == null ? Utils.findAnyShell()
					: soParent.getComposite();

			String skinRef = getSkinRef();
			if (skinRef != null) {
				Shell shell = parent.getShell();
				Cursor cursor = shell.getCursor();
				try {
					shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

					// wrap skinRef with a container that we control visibility of
					// (invisible by default)
					SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
							"MdiContents." + uniqueNumber++, SO_ID_ENTRY_WRAPPER,
							getParentSkinObject(), null);

					SWTSkinObject skinObject = skin.createSkinObject(id, skinRef,
							soContents, getDatasourceCore());

					control = skinObject.getControl();
					control.setLayoutData(Utils.getFilledFormData());
					control.getParent().layout(true, true);
					setPluginSkinObject(skinObject);
					initialize((Composite) control);
					setSkinObjectMaster(soContents);
				} finally {
					shell.setCursor(cursor);
				}
			} else {
				// XXX: This needs to be merged into BaseMDIEntry.initialize
				try {
					SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
							"MdiIView." + uniqueNumber++, SO_ID_ENTRY_WRAPPER,
							getParentSkinObject());

					parent.setBackgroundMode(SWT.INHERIT_NONE);

					Composite viewComposite = soContents.getComposite();
					boolean doGridLayout = true;
					if (getControlType() == CONTROLTYPE_SKINOBJECT) {
						doGridLayout = false;
					}
					//					viewComposite.setBackground(parent.getDisplay().getSystemColor(
					//							SWT.COLOR_WIDGET_BACKGROUND));
					//					viewComposite.setForeground(parent.getDisplay().getSystemColor(
					//							SWT.COLOR_WIDGET_FOREGROUND));
					if (doGridLayout) {
						GridLayout gridLayout = new GridLayout();
						gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
						viewComposite.setLayout(gridLayout);
						viewComposite.setLayoutData(Utils.getFilledFormData());
					}

					setPluginSkinObject(soContents);
					initialize(viewComposite);
					String fullTitle = getFullTitle();
					if (fullTitle != null && PAINT_BG) {
						swtItem.setText(getFullTitle());
					}

					Composite iviewComposite = getComposite();
					control = iviewComposite;
					// force layout data of IView's composite to GridData, since we set
					// the parent to GridLayout (most plugins use grid, so we stick with
					// that instead of form)
					if (doGridLayout) {
						Object existingLayoutData = iviewComposite.getLayoutData();
						Object existingParentLayoutData = iviewComposite.getParent().getLayoutData();
						if (existingLayoutData == null
								|| !(existingLayoutData instanceof GridData)
								&& (existingParentLayoutData instanceof GridLayout)) {
							GridData gridData = new GridData(GridData.FILL_BOTH);
							iviewComposite.setLayoutData(gridData);
						}
					}

					parent.layout(true, true);

					setSkinObjectMaster(soContents);
				} catch (Exception e) {
					Debug.out("Error creating sidebar content area for " + id, e);
					close(true);
				}

			}

			if (control != null && !control.isDisposed()) {
				control.setData("BaseMDIEntry", this);
				control.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent e) {
						close(true);
					}
				});
			} else {
				return false;
			}
		} // control == null

		return true;
	}
	
	public boolean
	isReallyDisposed()
	{
		return( swtItem == null || swtItem.isDisposed());
	}

	/* (non-Javadoc)
	 * @see BaseMdiEntry#show()
	 */
	@Override
	public void show() {
		// ensure show order by user execThreadLater
		// fixes case where two showEntries are called, the first from a non
		// SWT thread, and the 2nd from a SWT thread.  The first one will run last
		// showing itself
		Utils.execSWTThreadLater(0, new AERunnable() {
			@Override
			public void runSupport() {
				swt_show();
			}
		});
	}

	private void swt_show() {
		if (swtItem == null) {
			showonSWTItemSet = true;
			return;
		}
		showonSWTItemSet = false;
		if (!swt_build()) {
			return;
		}

		triggerOpenListeners();

		swtItem.getParent().select(swtItem);
		swtItem.getParent().showItem(swtItem);

		super.show();
	}

	@Override
	public void hide() {
		// if we defer the show above then we should defer the hide similarly to ensure that a caller trying
		// to show a new view before hiding the old (to avoid an intermediate blank view) ends up executing things
		// in teh desired order
		Utils.execSWTThreadLater(0, new AERunnable() {
			@Override
			public void runSupport() {
				SideBarEntrySWT.super.hide();
			}
		});
	}

	protected void swt_paintSideBar(Event event) {
		neverPainted = false;
		//System.out.println(System.currentTimeMillis() + "] paint " + getId() + ";sel? " + ((event.detail & SWT.SELECTED) > 0));
		TreeItem treeItem = (TreeItem) event.item;
		if (treeItem.isDisposed() || isDisposed()) {
			return;
		}
		Rectangle itemBounds = treeItem.getBounds();
		Rectangle drawBounds = event.gc.getClipping();
		if (drawBounds.isEmpty()) {
			drawBounds = event.getBounds();
		}
		Rectangle treeArea = treeItem.getParent().getClientArea();
		if (Utils.isGTK3) {
			// workaround bug
  		if (treeArea.width > itemBounds.width) {
  			itemBounds.width = treeArea.width;
  		}
  		if (treeArea.x < itemBounds.x) {
  			itemBounds.x = treeArea.x;
  		}
  		drawBounds = itemBounds;
		}

		String text = getTitle();
		if (text == null)
			text = "";


		//Point size = event.gc.textExtent(text);
		//Rectangle treeBounds = tree.getBounds();
		GC gc = event.gc;

		gc.setAntialias(SWT.ON);
		gc.setAdvanced(true);
		Utils.setClipping(gc, null);

		boolean selected = (event.detail & SWT.SELECTED) > 0;
		Color fgText = swt_paintEntryBG(event.detail, gc, drawBounds);

		Tree tree = (Tree) event.widget;

		Font font = tree.getFont();
		if (font != null && !font.isDisposed()) {
			gc.setFont(font);
		}

		if (SideBar.USE_NATIVE_EXPANDER && Utils.isGTK3) {
			itemBounds.x = treeItem.getBounds().x;
		} else if (DO_OUR_OWN_TREE_INDENT) {
			TreeItem tempItem = treeItem.getParentItem();
			int indent;
			if (tempItem == null && !Utils.isGTK) {
				indent = EXPANDO_INDENT_INITIAL;
			} else {
				indent = DO_EXPANDO_INDENT?EXPANDO_INDENT_INITIAL:EXPANDO_LEFT_INDENT;
			}
			while (tempItem != null) {
				indent += EXPANDO_INDENT;
				tempItem = tempItem.getParentItem();
			}
			if (SideBar.USE_NATIVE_EXPANDER && Utils.isGTK) {
				indent += 5;
			}
			
			if ( treeItem.getItemCount() > 0	&& !SideBar.USE_NATIVE_EXPANDER) {
				// expando visible
			}else{
				if ( COMPACT_SIDEBAR ){
					indent -= EXPANDO_INDENT/2;
				}
			}
			itemBounds.x = indent;
		}
		int x1IndicatorOfs;
		int x0IndicatorOfs = itemBounds.x;

		if  ( CLOSE_IMAGE_POSITION == 2 ){
			
				// never 
			
			x1IndicatorOfs = 0;
			
		}else if ( CLOSE_IMAGE_POSITION == 1 ){
			
				// on right 
			
			x1IndicatorOfs = 0;
			
			if (isCloseable()) {
				Image img = selected ? imgCloseSelected : imgClose;
				Rectangle closeArea = img.getBounds();
				closeArea.x = treeArea.width - closeArea.width - SIDEBAR_SPACING
						- x1IndicatorOfs;
				closeArea.y = itemBounds.y + (itemBounds.height - closeArea.height) / 2;
				x1IndicatorOfs += closeArea.width + SIDEBAR_SPACING;

				//gc.setBackground(treeItem.getBackground());
				//gc.fillRectangle(closeArea);

				gc.drawImage(img, closeArea.x, closeArea.y);
				treeItem.setData("closeArea", closeArea);
			}else{
				x1IndicatorOfs += imgClose.getBounds().width + SIDEBAR_SPACING;
			}
		}else{
			
			x1IndicatorOfs = SIDEBAR_SPACING;
		}
		
		
		//System.out.println(System.currentTimeMillis() + "] refresh " + getId() + "; " + itemBounds + ";clip=" + event.gc.getClipping() + ";eb=" + event.getBounds());
		if (viewTitleInfo != null) {
			String textIndicator = null;
			try {
				textIndicator = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
			} catch (Exception e) {
				Debug.out(e);
			}
			if (textIndicator != null) {

				Point textSize = gc.textExtent(textIndicator);
				//Point minTextSize = gc.textExtent("99");
				//if (textSize.x < minTextSize.x + 2) {
				//	textSize.x = minTextSize.x + 2;
				//}

				int width = textSize.x + 10;
				x1IndicatorOfs += width + SIDEBAR_SPACING;
				int startX = treeArea.width - x1IndicatorOfs;

				int textOffsetY = 0;

				int height = textSize.y + 1;
				int startY = itemBounds.y + (itemBounds.height - height) / 2;

				//gc.fillRectangle(startX, startY, width, height);

				//Pattern pattern;
				//Color color1;
				//Color color2;

				Color default_color = ColorCache.getSchemedColor(gc.getDevice(), "#5b6e87");

				Object color =  viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_COLOR);

				if ( color instanceof int[] ){

					gc.setBackground(ColorCache.getColor( gc.getDevice(),(int[])color ));

				}else{

					gc.setBackground( default_color );
				}


				/*
				if (selected) {
					color1 = ColorCache.getColor(gc.getDevice(), colors[0]);
					color2 = ColorCache.getColor(gc.getDevice(), colors[1]);
					pattern = new Pattern(gc.getDevice(), 0, startY, 0, startY + height,
							color1, 127, color2, 4);
				} else {
					color1 = ColorCache.getColor(gc.getDevice(), colors[2]);
					color2 = ColorCache.getColor(gc.getDevice(), colors[3]);
					pattern = new Pattern(gc.getDevice(), 0, startY, 0, startY + height,
							color1, color2);
				}
				gc.setBackgroundPattern(pattern);
				*/

				Color text_color = Colors.white;

				gc.fillRoundRectangle(startX, startY, width, height, textSize.y * 2 / 3,
						height * 2 / 3);

				if ( color != null ){

					Color bg = gc.getBackground();

					int	red 	= bg.getRed();
					int green 	= bg.getGreen();
					int blue	= bg.getBlue();

					double brightness = Math.sqrt( red*red*0.299+green*green*0.587+blue*blue*0.114);

					if ( brightness >= 130 ){
						text_color = Colors.black;
					}

					gc.setBackground( default_color );

					gc.drawRoundRectangle(startX, startY, width, height, textSize.y * 2 / 3,
							height * 2 / 3);
				}
				//gc.setBackgroundPattern(null);
				//pattern.dispose();
				if (maxIndicatorWidth > width) {
					maxIndicatorWidth = width;
				}
				gc.setForeground(text_color);
				GCStringPrinter.printString(gc, textIndicator, new Rectangle(startX,
						startY + textOffsetY, width, height), true, false, SWT.CENTER);
			}
		}

		//if (x1IndicatorOfs < 30) {
		//	x1IndicatorOfs = 30;
		//}

		if ( CLOSE_IMAGE_POSITION == 0 ){
			
			if (isCloseable()) {
				Image img = selected ? imgCloseSelected : imgClose;
				Rectangle closeArea = img.getBounds();
				closeArea.x = treeArea.width - closeArea.width - SIDEBAR_SPACING
						- x1IndicatorOfs;
				closeArea.y = itemBounds.y + (itemBounds.height - closeArea.height) / 2;
				x1IndicatorOfs += closeArea.width + SIDEBAR_SPACING;
	
				//gc.setBackground(treeItem.getBackground());
				//gc.fillRectangle(closeArea);
	
				gc.drawImage(img, closeArea.x, closeArea.y);
				treeItem.setData("closeArea", closeArea);
			}
		}
		
		MdiEntryVitalityImage[] vitalityImages = getVitalityImages();
		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
			if (vitalityImage == null || !vitalityImage.isVisible()
					|| vitalityImage.getAlignment() != SWT.RIGHT) {
				continue;
			}
			vitalityImage.switchSuffix(selected ? "-selected" : "");
			Image image = vitalityImage.getImage();
			if (image != null && !image.isDisposed()) {
				Rectangle bounds = image.getBounds();
				bounds.x = treeArea.width - bounds.width - SIDEBAR_SPACING
						- x1IndicatorOfs;
				bounds.y = itemBounds.y + (itemBounds.height - bounds.height) / 2;
				x1IndicatorOfs += bounds.width + SIDEBAR_SPACING;

				gc.drawImage(image, bounds.x, bounds.y);
				// setHitArea needs it relative to entry
				bounds.y -= itemBounds.y;
				vitalityImage.setHitArea(bounds);
			}
		}

		boolean greyScale = false;

		if (viewTitleInfo != null) {

			Object active_state = viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_ACTIVE_STATE);

			if (active_state instanceof Long) {

				greyScale = (Long) active_state == 2;
			}
		}

		String suffix = selected ? "-selected" : null;
		Image imageLeft = getImageLeft(suffix);
		if (imageLeft == null && selected) {
			releaseImageLeft(suffix);
			suffix = null;
			imageLeft = getImageLeft(null);
		}
		
		if ( IMAGELEFT_HIDDEN ){
			
		}else if (imageLeft != null ) {
			Rectangle clipping = gc.getClipping();
			Utils.setClipping(gc, new Rectangle(x0IndicatorOfs, itemBounds.y, IMAGELEFT_SIZE,
					itemBounds.height));

			if (greyScale) {
				greyScale = false;
				String imageLeftID = getImageLeftID();
				if (imageLeftID != null) {
					Image grey = ImageLoader.getInstance().getImage(imageLeftID + "-gray");

					if (grey != null) {
						imageLeft = grey;
						gc.setAlpha(160);
						greyScale = true;
					}
				}
			}

			Rectangle bounds = imageLeft.getBounds();
			int w = bounds.width;
			int h = bounds.height;
			if (w > IMAGELEFT_SIZE) {
				float pct = IMAGELEFT_SIZE / (float) w;
				w = IMAGELEFT_SIZE;
				h *= pct;
			}
			int x = x0IndicatorOfs + ((IMAGELEFT_SIZE - w) / 2);
			int y = itemBounds.y + ((itemBounds.height - h) / 2);

			gc.setAdvanced(true);
			gc.setInterpolation(SWT.HIGH);
			gc.drawImage(imageLeft, 0, 0, bounds.width, bounds.height, x, y, w, h );

			if (greyScale) {
				String imageLeftID = getImageLeftID();
  			gc.setAlpha(255);
  			ImageLoader.getInstance().releaseImage(imageLeftID + "-gray");
			}

			releaseImageLeft(suffix);
			Utils.setClipping(gc, clipping);
			//			0, 0, bounds.width, bounds.height,
			//					x0IndicatorOfs, itemBounds.y
			//							+ ((itemBounds.height - IMAGELEFT_SIZE) / 2), IMAGELEFT_SIZE,
			//					IMAGELEFT_SIZE);

			x0IndicatorOfs += IMAGELEFT_SIZE + IMAGELEFT_GAP;

			releaseImageLeft(suffix);
		} else if (ALWAYS_IMAGE_GAP) {
			if (isSelectable()) {
				x0IndicatorOfs += IMAGELEFT_SIZE + IMAGELEFT_GAP;
			}
		} else {
			if (treeItem.getParentItem() != null) {
				x0IndicatorOfs += 30 - 18;
			}
		}

		// Main Text
		////////////

		Rectangle clipping = new Rectangle(x0IndicatorOfs, itemBounds.y,
				treeArea.width - x1IndicatorOfs - SIDEBAR_SPACING - x0IndicatorOfs,
				itemBounds.height);

		if (drawBounds.intersects(clipping)) {
			int style= SWT.NONE;
  		if (!isSelectable()) {
  			Font headerFont = sidebar.getHeaderFont();
  			if (headerFont != null && !headerFont.isDisposed()) {
  				gc.setFont(headerFont);
  			}
  			//text = text.toUpperCase();

    		gc.setForeground(ColorCache.getColor(gc.getDevice(), 255, 255, 255));
    		gc.setAlpha(100);
    		clipping.x++;
    		clipping.y++;
    		//style = SWT.TOP;
  			GCStringPrinter sp = new GCStringPrinter(gc, text, clipping, true, false,
  					style);
  			sp.printString();
    		gc.setAlpha(255);

    		clipping.x--;
    		clipping.y--;
  			gc.setForeground(fgText);
  		} else {
  			if ( treeItem.getItemCount() > 0 || id.equals( MultipleDocumentInterface.SIDEBAR_HEADER_DASHBOARD )){
  				Font headerFont = sidebar.getHeaderFont();
  	  			if (headerFont != null && !headerFont.isDisposed()) {
  	  				gc.setFont(headerFont);
  	  			}
  			}
  			gc.setForeground(fgText);
  		}
			//Utils.setClipping(gc, clipping);

			GCStringPrinter sp = new GCStringPrinter(gc, text, clipping, true, false,
					style);
			sp.printString();
			clipping.x += sp.getCalculatedSize().x + 5;
			//Utils.setClipping(gc, (Rectangle) null);
		}

		// Vitality Images

		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
			if (!vitalityImage.isVisible()
					|| vitalityImage.getAlignment() != SWT.LEFT) {
				continue;
			}
			vitalityImage.switchSuffix(selected ? "-selected" : "");
			Image image = vitalityImage.getImage();
			if (image != null && !image.isDisposed()) {
				Rectangle bounds = image.getBounds();
				bounds.x = clipping.x;
				bounds.y = itemBounds.y + (itemBounds.height - bounds.height) / 2;
				clipping.x += bounds.width + SIDEBAR_SPACING;

				if (clipping.x > (treeArea.width - x1IndicatorOfs)) {
					vitalityImage.setHitArea(null);
					continue;
				}
				gc.drawImage(image, bounds.x, bounds.y);
				vitalityImage.setHitArea(bounds);
			}
		}

		// EXPANDO

		// OSX overrides the twisty, and we can't use the default twisty
		// on Windows because it doesn't have transparency and looks ugly
		if (treeItem.getItemCount() > 0	&& !SideBar.USE_NATIVE_EXPANDER) {
			gc.setAntialias(SWT.ON);
			Color oldBG = gc.getBackground();
			gc.setBackground(Colors.getSystemColor(event.display, SWT.COLOR_LIST_FOREGROUND));
			int baseX = DO_EXPANDO_INDENT?itemBounds.x:EXPANDO_INDENT_INITIAL;
			if (treeItem.getExpanded()) {
				int xStart = EXPANDO_WIDTH;
				int arrowSize = 8;
				int yStart = itemBounds.height - (itemBounds.height + arrowSize) / 2;
				gc.fillPolygon(new int[] {
					baseX - xStart,
					itemBounds.y + yStart,
					baseX - xStart + arrowSize,
					itemBounds.y + yStart,
					baseX - xStart + (arrowSize / 2),
					itemBounds.y + yStart + arrowSize,
				});
			} else {
				int xStart = EXPANDO_WIDTH;
				int arrowSize = 8;
				int yStart = itemBounds.height - (itemBounds.height + arrowSize) / 2;
				gc.fillPolygon(new int[] {
					baseX - xStart,
					itemBounds.y + yStart,
					baseX - xStart + arrowSize,
					itemBounds.y + yStart + 4,
					baseX - xStart,
					itemBounds.y + yStart + 8,
				});
			}
			gc.setBackground(oldBG);
			Font headerFont = sidebar.getHeaderFont();
			if (headerFont != null && !headerFont.isDisposed()) {
				gc.setFont(headerFont);
			}
		}
	}

	protected Color swt_paintEntryBG(int detail, GC gc, Rectangle drawBounds) {
		neverPainted = false;
		Color fgText = Colors.black;
		boolean selected = (detail & SWT.SELECTED) > 0;
		//boolean focused = (detail & SWT.FOCUSED) > 0;
		boolean hot = (detail & SWT.HOT) > 0;
		if (selected) {
			attention_start = -1;
		}else{
			if ( attention_start != -1 && attention_flash_on ){
				selected = true;
			}
		}
		if (selected) {
			if (!PAINT_BG) {
  			//gc.fillRectangle(drawBounds.x, drawBounds.y, drawBounds.width, drawBounds.height);
  			fgText = gc.getForeground();
			} else {
  			//System.out.println("gmmm" + drawBounds + ": " + Debug.getCompressedStackTrace());
				Utils.setClipping(gc, (Rectangle) null);
  			if (fgSel != null) {
  				fgText = fgSel;
  			}
  			if (bgSel != null) {
  				gc.setBackground(bgSel);
  			}
  			Color color1;
  			Color color2;
  			if (sidebar.getTree().isFocusControl()) {
  				color1 = ColorCache.getSchemedColor(gc.getDevice(), "#166688");
  				color2 = ColorCache.getSchemedColor(gc.getDevice(), "#1c2458");
  			} else {
  				color1 = ColorCache.getSchemedColor(gc.getDevice(), "#447281");
  				color2 = ColorCache.getSchemedColor(gc.getDevice(), "#393e58");
  			}

  			gc.setBackground(color1);
  			gc.fillRectangle(drawBounds.x, drawBounds.y, drawBounds.width, 4);

  			gc.setForeground(color1);
  			gc.setBackground(color2);
  			Rectangle itemBounds = swt_getBounds();
  			if (itemBounds == null) {
  				return fgText;
  			}
  			// always need to start gradient at the same Y position
  			// +3 is to start gradient off 3 pixels lower
  			gc.fillGradientRectangle(drawBounds.x, itemBounds.y + 3,
  					drawBounds.width, itemBounds.height - 3, true);
			}
		} else {

			if (fg != null) {
				fgText = fg;
			}
			if (bg != null) {
				gc.setBackground(bg);
			}

			if (this == sidebar.draggingOver || hot) {
				Color c = skin.getSkinProperties().getColor("color.sidebar.drag.bg");
				gc.setBackground(c);
			}

			if (PAINT_BG) {
				gc.fillRectangle(drawBounds);
			}

			if (this == sidebar.draggingOver) {
				Color c = skin.getSkinProperties().getColor("color.sidebar.drag.fg");
				gc.setForeground(c);
				gc.setLineWidth(5);
				gc.drawRectangle(drawBounds);
			}
		}
		return fgText;
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		ImageLoader imageLoader = ImageLoader.getInstance();
		if (imageLoader != null) {
			imageLoader.releaseImage("image.sidebar.closeitem");
			imageLoader.releaseImage("image.sidebar.closeitem-selected");
		}

		setDisposed(true);

		final TreeItem treeItem = (TreeItem) e.widget;
		if (treeItem != swtItem) {
			Debug.out("Warning: TreeItem changed for sidebar " + id);
			return;
		}

		if (swtItem == null) {
			return;
		}

		if (swtItem != null && !Constants.isOSX) {
			// In theory, the disposal of swtItem will trigger the disposal of the
			// children.  Let's force it just in case
			// On OSX this will cause disposal confusion in SWT, and possibly result
			// in a SIGSEGV crash.
			TreeItem[] children = swtItem.getItems();
			for (TreeItem child : children) {
				if (child.isDisposed()) {
					continue;
				}
				MdiEntry entry = (MdiEntry) child.getData("MdiEntry");
				if (entry != null) {
					entry.close(true);
				}
			}
		}

		final Tree tree = sidebar.getTree();

			// swtItem can get set to null between the above test and here...

		if (tree.isDisposed() || ( swtItem != null && swtItem.isDisposed()) || tree.getShell().isDisposed()) {
			return;
		}

		setTreeItem(null);

		mdi.removeItem(SideBarEntrySWT.this);

		SWTThread instance = SWTThread.getInstance();
		
		boolean user = instance != null && !instance.isTerminated();
		
		if ( user ){
			
			if ( closeWasUserInitiated != null ){
				
				user = closeWasUserInitiated;
				
			}else{
			
					// It's not a user close if the parent is making the children (this entry)
					// close.  parent will be marked disposed, so use that as a check.
		  		String parentID = getParentID();
		  		if (parentID != null) {
		  			MdiEntry entry = mdi.getEntry(parentID);
		  			if (entry != null && entry.isDisposed()) {
		  				user = false;
		  			}
		  		}
			}
		}
		
		triggerCloseListeners(user);

		SWTSkinObject so = getSkinObject();
		if (so != null) {
			setSkinObjectMaster(null);
			so.getSkin().removeSkinObject(so);
		}

		for (SideBarVitalityImageSWT vitalityImage : listVitalityImages) {
			vitalityImage.dispose();
		}
		listVitalityImages.clear();

		// delay saving of removing of auto-open flag.  If after the delay, we are
		// still alive, it's assumed the user invoked the close, and we should
		// remove the auto-open flag
		Utils.execSWTThreadLater(0, new SWTRunnable() {
			@Override
			public void runWithDisplay(Display display) {
				// even though execThreadLater will not run on close of app because
				// the display is disposed, do a double check of tree disposal just
				// in case.  We don't want to trigger close listeners or
				// remove autoopen parameters if the user is closing the app (as
				// opposed to closing  the sidebar)
				if (tree.isDisposed()) {
					return;
				}

				try {
					COConfigurationManager.removeParameter("SideBar.AutoOpen." + id);

					// OSX doesn't select a treeitem after closing an existing one
					// Force selection
					if (Constants.isOSX && !tree.isDisposed()
							&& tree.getSelectionCount() == 0) {

						String parentid = getParentID();
						if (parentid != null && mdi.getEntry(parentid) != null) {
							mdi.showEntryByID(parentid);
						} else {
							mdi.showEntryByID(SideBar.SIDEBAR_SECTION_LIBRARY);
						}
					}
				} catch (Exception e2) {
					Debug.out(e2);
				}

					// See if this entry has been replaced by another in the meantime. This happens when we are
					// moving an entry in the sidebar by removing it and then re-adding it. We assume that the
					// auto-open properties of the replacement are the same as those of the initial entry

				boolean	replaced = false;

				String my_id = SideBarEntrySWT.this.getId();

				if ( my_id != null ){

					MdiEntry entry = mdi.getEntry( my_id );

					if ( entry != null && entry != SideBarEntrySWT.this ){

						replaced = true;
					}
				}

				if ( !replaced ){

					mdi.removeEntryAutoOpen(id);
				}
			}
		});
	}

	public void setParentSkinObject(SWTSkinObjectContainer soParent) {
		this.soParent = soParent;
	}

	public SWTSkinObjectContainer getParentSkinObject() {
		return soParent;
	}

	@Override
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
		updateColors();
	}

	@Override
	public boolean isSelectable() {
		return selectable;
	}

	public boolean swt_isVisible() {
		TreeItem parentItem = swtItem.getParentItem();
		if (parentItem != null) {
			MdiEntry parentEntry = (MdiEntry) parentItem.getData("MdiEntry");
			if (!parentEntry.isExpanded()) {
				return false;
			}
		}
		return true; // todo: bounds check
	}

	// @see com.biglybt.ui.swt.debug.ObfuscateImage#obfuscatedImage(org.eclipse.swt.graphics.Image)
	@Override
	public Image obfuscatedImage(Image image) {
		Rectangle bounds = swt_getBounds();
		if ( bounds != null ){
			TreeItem treeItem = getTreeItem();
			Point location = Utils.getLocationRelativeToShell(treeItem.getParent());

			bounds.x += location.x;
			bounds.y += location.y;

			Map<String, Object> map = new HashMap<>();
			map.put("image", image);
			map.put("obfuscateTitle", false);
			triggerEvent(UISWTViewEvent.TYPE_OBFUSCATE, map);

			if (viewTitleInfo instanceof ObfuscateImage) {
				((ObfuscateImage) viewTitleInfo).obfuscatedImage(image);
			}

			int ofs = IMAGELEFT_GAP + IMAGELEFT_SIZE;
			if (treeItem.getParentItem() != null) {
				ofs += 10 + SIDEBAR_SPACING;
			}
			bounds.x += ofs;
			bounds.width -= ofs + SIDEBAR_SPACING + 1;
			bounds.height -= 1;

			if (viewTitleInfo instanceof ObfuscateTab) {
				String header = ((ObfuscateTab) viewTitleInfo).getObfuscatedHeader();
				if (header != null) {
					UIDebugGenerator.obfuscateArea(image, bounds, header);
				}
			}

			if (MapUtils.getMapBoolean(map, "obfuscateTitle", false)) {
				UIDebugGenerator.obfuscateArea(image, bounds);
			}
		}

		return image;
	}

	// @see BaseMdiEntry#setToolbarVisibility(boolean)
	@Override
	protected void setToolbarVisibility(boolean visible) {
		if (toolBarInfoBar != null) {
			if (visible) {
				toolBarInfoBar.show();
			} else {
				toolBarInfoBar.hide(false);
			}
			return;
		}
		SWTSkinObject soMaster = getSkinObjectMaster();
		if (soMaster == null) {
			return;
		}
		SWTSkinObject so = getSkinObject();
		if (so == null) {
			return;
		}
		SWTSkinObject soToolbar = skin.getSkinObject(SkinConstants.VIEWID_VIEW_TOOLBAR, soMaster);
		if (soToolbar == null && visible) {
			toolBarInfoBar = new InfoBarUtil(so, SO_ID_TOOLBAR, true, "", "") {
				@Override
				public boolean allowShow() {
					return true;
				}
			};
		} else if (soToolbar != null) {
			soToolbar.setVisible(visible);
		}
	}

	/* (non-Javadoc)
	 * @see BaseMdiEntry#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String title) {
		super.setTitle(title);

		refreshTitle();
	}

	@Override
	protected void
	refreshTitle()
	{
		if (!PAINT_BG) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {

			@Override
			public void runSupport() {
				if (swtItem == null || swtItem.isDisposed()) {
					return;
				}
				String title = getTitle();

				if ( !swtItem.getText().equals( title )){

					swtItem.setText( title );

					redraw();
				}
			}
		});
	}
}
