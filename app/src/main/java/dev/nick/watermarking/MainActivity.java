package dev.nick.watermarking;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.FileNotFoundException;

import dev.nick.android.imageloader.ImageLoader;
import dev.nick.android.imageloader.ui.DisplayOption;
import dev.nick.android.imageloader.ui.MediaQuality;
import dev.nick.android.injection.annotation.binding.BindView;

public class MainActivity extends PickerActivity {

    @BindView(R.id.fab_camera)
    FloatingActionButton mFabCam;
    @BindView(R.id.fab_gallery)
    FloatingActionButton mFabGal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bindFabs();
    }

    void bindFabs() {
        FloatingActionsMenu fm = (FloatingActionsMenu) findViewById(R.id.fab);
        mFabCam = (FloatingActionButton) fm.findViewById(R.id.fab_camera);
        mFabCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTakePhotoActivity();
            }
        });
        mFabGal = (FloatingActionButton) fm.findViewById(R.id.fab_gallery);
        mFabGal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startPickFromGalleryActivity();
            }
        });
    }

    @Override
    protected void onPhotoSelected(Uri uri) {
        super.onPhotoSelected(uri);
        final ImageView view = (ImageView) findViewById(R.id.image);
        try {
            view.setImageBitmap(getBitmapFromUri(this, uri));
        } catch (FileNotFoundException e) {

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
