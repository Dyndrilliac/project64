/****************************************************************************
*                                                                           *
* Project64 - A Nintendo 64 emulator.                                       *
* http://www.pj64-emu.com/                                                  *
* Copyright (C) 2012 Project64. All rights reserved.                        *
*                                                                           *
* License:                                                                  *
* GNU/GPLv2 http://www.gnu.org/licenses/gpl-2.0.html                        *
*                                                                           *
****************************************************************************/
package emu.project64;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import emu.project64.R;
import emu.project64.dialog.ProgressDialog;
import emu.project64.game.GameActivity;
import emu.project64.game.GameActivityXperiaPlay;
import emu.project64.inAppPurchase.IabBroadcastReceiver;
import emu.project64.inAppPurchase.IabBroadcastReceiver.IabBroadcastListener;
import emu.project64.inAppPurchase.IabHelper;
import emu.project64.inAppPurchase.IabHelper.IabAsyncInProgressException;
import emu.project64.inAppPurchase.IabResult;
import emu.project64.inAppPurchase.Inventory;
import emu.project64.inAppPurchase.Purchase;
import emu.project64.jni.NativeExports;
import emu.project64.jni.SettingsID;
import emu.project64.jni.SystemEvent;
import emu.project64.jni.UISettingID;
import emu.project64.settings.GameSettingsActivity;
import emu.project64.settings.SettingsActivity;
import emu.project64.util.Utility;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

public class GalleryActivity extends AppCompatActivity implements IabBroadcastListener
{
    //Progress dialog for ROM scan
    private ProgressDialog mProgress = null;

    // Widgets
    private RecyclerView mGridView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private MenuListView mDrawerList;

    // Resizable gallery thumb nails
    public int galleryWidth;
    public int galleryMaxWidth;
    public int galleryHalfSpacing;
    public int galleryColumns = 2;

    // Misc.
    private static List<GalleryItem> mGalleryItems = new ArrayList<GalleryItem>();
    private static List<GalleryItem> mRecentItems = new ArrayList<GalleryItem>();
    private static GalleryActivity mActiveGalleryActivity = null;

    // The IAB helper object
    IabHelper mIabHelper;
    private boolean mHasSaveSupport = false;

    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;

    public static final int GAME_DIR_REQUEST_CODE = 1;
    static final String SKU_SAVESUPPORT = "save_support";

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;
    static final int RC_SETTINGS = 10002;

    @Override
    protected void onNewIntent( Intent intent )
    {
        // If the activity is already running and is launched again (e.g. from a file manager app),
        // the existing instance will be reused rather than a new one created. This behavior is
        // specified in the manifest (launchMode = singleTask). In that situation, any activities
        // above this on the stack (e.g. GameActivity, GamePrefsActivity) will be destroyed
        // gracefully and onNewIntent() will be called on this instance. onCreate() will NOT be
        // called again on this instance. Currently, the only info that may be passed via the intent
        // is the selected game path, so we only need to refresh that aspect of the UI. This will
        // happen anyhow in onResume(), so we don't really need to do much here.
        super.onNewIntent( intent );

        // Only remember the last intent used
        setIntent( intent );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        mActiveGalleryActivity = this;

        mIabHelper = new IabHelper(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnfHFIq+X0oIvV+bwcvdqQv5GmpWLL6Bw8xE6MLFzXzUGUIUZBwQS6Cz5IC0UM76ujPDPqQPeGy/8oq/bswB5pHCz2iS4ySGalzFfYfeIDklOe+R1pLEqmHuwsR5o4b8rLePLGmUI7hA0kozOTb0i+epANV3Pj63i5XFZLA7RMi5I+YysoE9Fob6kCx0kb02AATacF0OXI9paE1izvsHhZcOIrT4TRMbGlZjBVE/pcJtoBDh33QKz/JBOXWvwnh+efqhVsq/UfA6jYI+U4Z4tsnWhem8DB6Kqj5EhClC6qCPmkBFiOabyKaqhI/urBtYOwxkW9erwtA6OcDoHm5J/JwIDAQAB");

        // enable debug logging (for a production application, you should set this to false).
        mIabHelper.enableDebugLogging(true);

        Log.d("GalleryActivity", "Starting setup.");
        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener()
        {
            public void onIabSetupFinished(IabResult result)
            {
                Log.d("GalleryActivity", "onIabSetupFinished.");

                if (!result.isSuccess())
                {
                    // Oh noes, there was a problem.
                    Log.d("GalleryActivity", "Problem setting up in-app billing: " + result);
                    // complain("Problem setting up in-app billing: " + result);
                    mHasSaveSupport = true;
                    return;
                }
                // Have we been disposed of in the meantime? If so, quit.
                if (mIabHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(GalleryActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d("GalleryActivity", "Setup successful. Querying inventory.");
                try
                {
                    mIabHelper.queryInventoryAsync(mGotInventoryListener);
                }
                catch (IabAsyncInProgressException e)
                {
                    //complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });

        // Lay out the content
        setContentView( R.layout.gallery_activity );
        mGridView = (RecyclerView) findViewById( R.id.gridview );
        mProgress = new ProgressDialog( null, this, getString( R.string.scanning_title ), "", getString( R.string.toast_pleaseWait ), false );

        // Load Cached Rom List
        NativeExports.LoadRomList();
        refreshGrid();

        // Update the grid layout
        galleryMaxWidth = (int) getResources().getDimension( R.dimen.galleryImageWidth );
        galleryHalfSpacing = (int) getResources().getDimension( R.dimen.galleryHalfSpacing );

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics( metrics );

        int width = metrics.widthPixels - galleryHalfSpacing * 2;
        galleryColumns = (int) Math.ceil( width * 1.0 / ( galleryMaxWidth + galleryHalfSpacing * 2 ) );
        galleryWidth = width / galleryColumns - galleryHalfSpacing * 2;

        GridLayoutManager layoutManager = (GridLayoutManager) mGridView.getLayoutManager();
        layoutManager.setSpanCount( galleryColumns );

        // Add the toolbar to the activity (which supports the fancy menu/arrow animation)
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        toolbar.setTitle( R.string.app_name );
        setSupportActionBar( toolbar );

        // Configure the navigation drawer
        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawerLayout );
        mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout, toolbar, 0, 0 );
        mDrawerLayout.setDrawerListener( mDrawerToggle );

        // Configure the list in the navigation drawer
        mDrawerList = (MenuListView) findViewById( R.id.drawerNavigation );
        mDrawerList.setMenuResource( R.menu.gallery_drawer );
        // Handle menu item selections
        mDrawerList.setOnClickListener( new MenuListView.OnClickListener()
        {
            @Override
            public void onClick( MenuItem menuItem )
            {
                GalleryActivity.this.onOptionsItemSelected( menuItem );
            }
        } );
    }

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set)
    {
        if (set)
        {
            WebView webView = (WebView)findViewById(R.id.screen_wait);
            webView.loadData(Utility.readAsset("loading.htm", ""), "text/html", "UTF8");
        }
        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener()
    {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory)
        {
            Log.d("GalleryActivity", "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mIabHelper == null) return;

            // Is it a failure?
            if (result.isFailure())
            {
                //complain("Failed to query inventory: " + result);
                return;
            }

            Log.d("GalleryActivity", "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            Purchase SaveSupportPurchase = inventory.getPurchase(SKU_SAVESUPPORT);
            Log.d("GalleryActivity", "Purchased save support " + (SaveSupportPurchase!= null ? "Yes" : "No"));
            if (SaveSupportPurchase != null)
            {
                mHasSaveSupport = true;
            }

            setWaitScreen(false);
        }
    };

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener()
    {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase)
        {
            Log.d("GalleryActivity", "Purchase finished: " + result + ", purchase: " + purchase);
            // if we were disposed of in the meantime, quit.
            if (mIabHelper == null) return;

            if (result.isFailure())
            {
                Log.e("GalleryActivity", "**** Purcahse Error: " + result);
                alert("Save Support Upgrade failed\n\n" + result.getMessage());
                setWaitScreen(false);
                return;
            }

            Log.d("GalleryActivity", "Purchase successful.");

            if (purchase.getSku().equals(SKU_SAVESUPPORT))
            {
                // bought the premium upgrade!
                Log.d("GalleryActivity", "Purchase is save support. Congratulating user.");
                alert("Thank you for upgrading to have save support!");
                mHasSaveSupport = true;
                setWaitScreen(false);
            }
        }
    };

    void alert(String message)
    {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d("GalleryActivity", "Showing alert dialog: " + message);
        bld.create().show();
    }

    public void receivedBroadcast()
    {
        // Received a broadcast notification that the inventory of items has changed
        Log.d("GalleryActivity", "Received broadcast notification. Querying inventory.");
        try
        {
            mIabHelper.queryInventoryAsync(mGotInventoryListener);
        }
        catch (IabAsyncInProgressException e)
        {
            //complain("Error querying inventory. Another async operation in progress.");
        }
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState )
    {
        super.onPostCreate( savedInstanceState );
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig )
    {
        super.onConfigurationChanged( newConfig );
        mDrawerToggle.onConfigurationChanged( newConfig );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.gallery_activity, menu );

        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch( item.getItemId() )
        {
            case R.id.menuItem_gameDir:
                Intent intent = new Intent(this, ScanRomsActivity.class);
                startActivityForResult( intent, GAME_DIR_REQUEST_CODE );
                return true;
            case R.id.menuItem_settings:
                Intent SettingsIntent = new Intent(this, SettingsActivity.class);
                startActivity( SettingsIntent );
                return true;
            case R.id.menuItem_forum:
                Intent ForumIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.pj64-emu.com/forumdisplay.php?f=13"));
                startActivity(ForumIntent);
                return true;
            case R.id.menuItem_reportBug:
                Intent IssueIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/project64/project64/issues"));
                startActivity(IssueIntent);
                return true;
            case R.id.menuItem_about:
                Intent AboutIntent = new Intent(this, AboutActivity.class);
                startActivity( AboutIntent );
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private boolean HasAutoSave(File GameSaveDir)
    {
        if (!GameSaveDir.exists() || !GameSaveDir.isDirectory())
        {
            return false;
        }

        File[] fList = GameSaveDir.listFiles();
        for (File file : fList)
        {
            String extension = "";

            int i = file.getName().lastIndexOf('.');
            if (i > 0)
            {
                extension = file.getName().substring(i+1);
            }
            if (extension.equals("zip"))
            {
                i = file.getName().lastIndexOf('.', i - 1);
                if (i > 0)
                {
                    extension = file.getName().substring(i+1);
                }
            }
            if (extension.equals("pj.zip") ||
                extension.equals("pj"))
            {
                return true;
            }
        }
        return false;
    }

    private void StartGameMenu (File GameSaveDir, boolean ShowSettings)
    {
        class Item
        {
            public final String text;
            public final int icon;
            public Item(String text, Integer icon)
            {
                this.text = text;
                this.icon = icon;
            }
            @Override
            public String toString()
            {
                return text;
            }
        }

        List<Item>menuItemLst = new ArrayList<Item>();
        if (mHasSaveSupport)
        {
            menuItemLst.add(new Item("Resume from Native save", R.drawable.ic_controller));
            menuItemLst.add(new Item("Resume from Auto save", R.drawable.ic_play));
        }
        else
        {
            menuItemLst.add(new Item("Resume from Native save", R.drawable.ic_lock));
            menuItemLst.add(new Item("Resume from Auto save", R.drawable.ic_lock));
        }
        menuItemLst.add(new Item("Restart", R.drawable.ic_refresh));
        if (ShowSettings && !NativeExports.SettingsLoadBool(SettingsID.UserInterface_BasicMode.getValue()))
        {
            menuItemLst.add(new Item("Settings", R.drawable.ic_sliders));
        }

        Item[] itemsDynamic = new Item[menuItemLst .size()];
        itemsDynamic = menuItemLst.toArray(itemsDynamic);

        final Item[] items = itemsDynamic;
        final File SaveDir = GameSaveDir;
        ListAdapter adapter = new ArrayAdapter<Item>( this, android.R.layout.select_dialog_item, android.R.id.text1, items)
        {
            public View getView(int position, View convertView, android.view.ViewGroup parent)
            {
                //Use super class to create the View
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView)v.findViewById(android.R.id.text1);

                // Get Drawable icon
                Drawable d = items[position].icon != 0 ? ResourcesCompat.getDrawable(getResources(), items[position].icon, null) : null;
                tv.setTextColor(Color.parseColor("#FFFFFF"));
                if (d != null)
                {
                    d.setColorFilter(Color.parseColor("#FFFFFF"), android.graphics.PorterDuff.Mode.SRC_ATOP);
                }
                if (!isEnabled(position))
                {
                    tv.setTextColor(Color.parseColor("#555555"));
                    if (d != null)
                    {
                        d.setColorFilter(Color.parseColor("#555555"), android.graphics.PorterDuff.Mode.SRC_ATOP);
                    }
                }

                //Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);

                //Add margin between image and text (support various screen densities)
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp5);

                return v;
            }

            @Override
            public boolean areAllItemsEnabled()
            {
                return true;
            }

            @Override
            public boolean isEnabled(int position)
            {
                if (position == 1 && HasAutoSave(SaveDir) == false)
                {
                    return false;
                }
                if (position == 2 && SaveDir.exists() == false)
                {
                    return false;
                }
                return true;
            }
        };

        final Context finalContext = this;
        final Activity finalActivity = this;
        AlertDialog.Builder GameMenu = new AlertDialog.Builder(finalContext);
        GameMenu.setTitle(NativeExports.SettingsLoadString(SettingsID.Game_GoodName.getValue()));
        GameMenu.setAdapter(adapter, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int item)
            {
                if ((item == 0 || item == 1) && !mHasSaveSupport)
                {
                    AlertDialog.Builder ResetPrompt = new AlertDialog.Builder(finalContext);
                    ResetPrompt
                    .setTitle(getText(R.string.GetSaveSupport_title))
                    .setMessage(getText(R.string.GetSaveSupport_message))
                    .setPositiveButton(R.string.GetSaveSupport_OkButton, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            setWaitScreen(true);
                            //Purchase save support
                            try
                            {
                                String payload = NativeExports.appVersion();
                                mIabHelper.launchPurchaseFlow(finalActivity, SKU_SAVESUPPORT, RC_REQUEST, mPurchaseFinishedListener, payload);
                            }
                            catch (IabAsyncInProgressException e)
                            {
                                setWaitScreen(false);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, this)
                    .show();
                    return;
                }
                if (item == 0)
                {
                    launchGameActivity();
                }
                else if (item == 1)
                {
                    NativeExports.SettingsSaveDword(SettingsID.Game_CurrentSaveState.getValue(), 0);
                    NativeExports.ExternalEvent(SystemEvent.SysEvent_LoadMachineState.getValue());
                    launchGameActivity();
                }
                else if (item == 2)
                {
                    AlertDialog.Builder ResetPrompt = new AlertDialog.Builder(finalContext);
                    ResetPrompt
                    .setTitle(getText(R.string.confirmResetGame_title))
                    .setMessage(getText(R.string.confirmResetGame_message))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            String[]entries = SaveDir.list();
                            for(String s: entries)
                            {
                                File currentFile = new File(SaveDir.getPath(),s);
                                currentFile.delete();
                            }
                            SaveDir.delete();
                            launchGameActivity();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, this)
                    .show();
                }
                else if (item == 3)
                {
                    Intent SettingsIntent = new Intent(finalContext, GameSettingsActivity.class);
                    startActivityForResult( SettingsIntent, RC_SETTINGS );
                }
            }
        });
        GameMenu.show();
    }

    public void onGalleryItemClick( GalleryItem item )
    {
        NativeExports.LoadGame(item.romFile.getAbsolutePath());
        File InstantSaveDir = new File(NativeExports.SettingsLoadString(SettingsID.Directory_InstantSave.getValue()));
        final File GameSaveDir = new File(InstantSaveDir,NativeExports.SettingsLoadString(SettingsID.Game_UniqueSaveDir.getValue()));
        if (GameSaveDir.exists() && !mHasSaveSupport)
        {
            StartGameMenu(GameSaveDir, false);
        }
        else
        {
            if (HasAutoSave(GameSaveDir))
            {
                NativeExports.SettingsSaveDword(SettingsID.Game_CurrentSaveState.getValue(), 0);
                NativeExports.ExternalEvent(SystemEvent.SysEvent_LoadMachineState.getValue());
            }
            launchGameActivity();
        }
    }

    public boolean onGalleryItemLongClick( GalleryItem item )
    {
        NativeExports.LoadGame(item.romFile.getAbsolutePath());
        File InstantSaveDir = new File(NativeExports.SettingsLoadString(SettingsID.Directory_InstantSave.getValue()));
        final File GameSaveDir = new File(InstantSaveDir,NativeExports.SettingsLoadString(SettingsID.Game_UniqueSaveDir.getValue()));

        StartGameMenu(GameSaveDir, true);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d("GalleryActivity", "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        if (requestCode == RC_SETTINGS)
        {
            File InstantSaveDir = new File(NativeExports.SettingsLoadString(SettingsID.Directory_InstantSave.getValue()));
            final File GameSaveDir = new File(InstantSaveDir,NativeExports.SettingsLoadString(SettingsID.Game_UniqueSaveDir.getValue()));

            StartGameMenu(GameSaveDir, true);
            return;
        }
        // Check which request we're responding to
        if (requestCode == GAME_DIR_REQUEST_CODE)
        {
            // Make sure the request was successful
            if (resultCode == RESULT_OK && data != null)
            {
                Bundle extras = data.getExtras();
                String searchPath = extras.getString( ScanRomsActivity.GAME_DIR_PATH );
                boolean searchRecursively = extras.getBoolean( ScanRomsActivity.GAME_DIR_RECURSIVELY );

                if (searchPath != null)
                {
                    NativeExports.RefreshRomDir(searchPath, searchRecursively);
                }
            }
        }
        // Pass on the activity result to the helper for handling
        if (mIabHelper != null && !mIabHelper.handleActivityResult(requestCode, resultCode, data))
        {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void refreshGrid( )
    {
        List<GalleryItem> items;
        items = new ArrayList<GalleryItem>();

        if (mRecentItems.size() > 0)
        {
            items.add( new GalleryItem( this, getString( R.string.galleryRecentlyPlayed ) ) );
            items.addAll( mRecentItems );

            items.add( new GalleryItem( this, getString( R.string.galleryLibrary ) ) );
        }
        items.addAll( mGalleryItems );

        mGridView.setAdapter( new GalleryItem.Adapter( this, items ) );

        // Allow the headings to take up the entire width of the layout
        final List<GalleryItem> finalItems = items;
        GridLayoutManager layoutManager = new GridLayoutManager( this, galleryColumns );
        layoutManager.setSpanSizeLookup( new GridLayoutManager.SpanSizeLookup()
        {
            @Override
            public int getSpanSize( int position )
            {
                // Headings will take up every span (column) in the grid
                if( finalItems.get( position ).isHeading )
                    return galleryColumns;

                // Games will fit in a single column
                return 1;
            }
        } );

        mGridView.setLayoutManager( layoutManager );
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        refreshViews();
    }

    @TargetApi( 11 )
    private void refreshViews()
    {
        // Refresh the gallery
        refreshGrid();
    }

    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_MENU )
        {
            if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
            {
                mDrawerLayout.closeDrawer( GravityCompat.START );
            }
            else
            {
                mDrawerLayout.openDrawer( GravityCompat.START );
            }
            return true;
        }
        return super.onKeyDown( keyCode, event );
    }

    @Override
    public void onBackPressed()
    {
        if( mDrawerLayout.isDrawerOpen( GravityCompat.START ) )
        {
            mDrawerLayout.closeDrawer( GravityCompat.START );
        }
        else
        {
            moveTaskToBack(true);
        }
    }

    public void launchGameActivity()
    {
        // Launch the game activity
        boolean isXperiaPlay = false;

        Intent intent = isXperiaPlay ? new Intent( this, GameActivityXperiaPlay.class ) : new Intent( this, GameActivity.class );
        this.startActivity( intent );
    }

    public static void RomListReset ()
    {
        mGalleryItems = new ArrayList<GalleryItem>();
        if (mActiveGalleryActivity != null && mActiveGalleryActivity.mProgress != null)
        {
            Handler h = new Handler(Looper.getMainLooper());
            h.post(new Runnable()
            {
                public void run()
                {
                    mActiveGalleryActivity.mProgress.show();
                }
            });
        }
    }

    public static void RomListAddItem (String FullFileName, String FileName, String GoodName, int TextColor)
    {
        GalleryItem item = new GalleryItem( mActiveGalleryActivity, GoodName, FileName, FullFileName, TextColor );
        mGalleryItems.add( item );
        if (mActiveGalleryActivity != null && mActiveGalleryActivity.mProgress != null)
        {
            Handler h = new Handler(Looper.getMainLooper());
            final String ProgressText = new String(FileName);
            final String ProgressSubText = new String(FullFileName);
            final String ProgressMessage = new String("Added " + GoodName);

            h.post(new Runnable()
            {
                public void run()
                {
                    mActiveGalleryActivity.mProgress.setText(ProgressText);
                    mActiveGalleryActivity.mProgress.setSubtext(ProgressSubText);
                    mActiveGalleryActivity.mProgress.setMessage(ProgressMessage);
                }
            });
        }
    }

    public static void RomListLoadDone()
    {
        mRecentItems = new ArrayList<GalleryItem>();

        Log.d("GalleryActivity","File_RecentGameFileCount = " + NativeExports.UISettingsLoadDword(UISettingID.File_RecentGameFileCount.getValue()));

        for (int i = 0, n = NativeExports.UISettingsLoadDword(UISettingID.File_RecentGameFileCount.getValue()); i < n; i++)
        {
            String RecentFile = NativeExports.UISettingsLoadStringIndex(UISettingID.File_RecentGameFileIndex.getValue(), i);
            if (RecentFile.length() == 0)
            {
                break;
            }
            for (int z = 0; z < mGalleryItems.size(); z++)
            {
                if (RecentFile.equals(mGalleryItems.get(z).romFile.getAbsolutePath()))
                {
                    mRecentItems.add(mGalleryItems.get(z));
                       break;
                }
            }
        }

        if (mActiveGalleryActivity != null && mActiveGalleryActivity.mProgress != null)
        {
            Handler h = new Handler(Looper.getMainLooper());
            h.post(new Runnable()
            {
                public void run()
                {
                    mActiveGalleryActivity.refreshGrid();
                    mActiveGalleryActivity.mProgress.dismiss();
                }
            });
        }
    }
}
