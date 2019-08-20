package com.gmail.katsaros.s.dimitris.e_ktima;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements android.widget.CompoundButton.OnCheckedChangeListener, android.widget.CompoundButton.OnClickListener {

    private static final String TAG = "MainActivity";

    Button registerBtn;
    Button searchBtn;
    Button emailBtn;
    Button exportBtn;
    Button importBtn;

    private DrawerLayout mDrawerLayout;
//    private NavigationView navigationView;

    private ArrayList<AreaInfo> areasList;

    private ListView mListView;
    private ArrayList<Card> cardsList;
    private CustomListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Permissions(MainActivity.this);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
//        navigationView = findViewById(R.id.nav_view);

        mListView = (ListView) findViewById(R.id.listView);

        registerBtn = (Button) findViewById(R.id.registerBtn);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (new Permissions(MainActivity.this).isPermissionsOK()) {
                    Log.d(TAG, "PERMISSIONS GRANTED");
                    Intent intent = new Intent(MainActivity.this, SaveAreaMap.class);
                    startActivity(intent);
                } else {
                    Log.d(TAG, "PERMISSIONS NOT GRANTED");
                }
            }
        });

        searchBtn = (Button) findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDrawer();
            }
        });

        emailBtn = (Button) findViewById(R.id.sent_via_email);
        emailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Sent via Email button", Toast.LENGTH_SHORT).show();

                ArrayList<Integer> countList = new ArrayList<>();
                if (cardsList != null) {
                    for (int i = 0; i < cardsList.size(); i++) {
                        if (cardsList.get(i).isCheckbox()) {
                            countList.add(i);
                        }
                    }

                    if (countList.size() != 0) {

                        Intent intent = new Intent(MainActivity.this, SendEmail.class);
                        intent.putExtra("areas", countList);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Δέν έχετε επιλέξει κάποιο αντικείμενο", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        importBtn = (Button) findViewById(R.id.import_button);
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ChooserDialog().with(MainActivity.this)
                        .withFilter(false, false, "json")
                        .withStartFile("")
                        .withChosenListener(new ChooserDialog.Result() {
                            @Override
                            public void onChoosePath(String path, File pathFile) {
                                // it takes a path and areas that you want to export
                                MyJSON.importFile(MainActivity.this, path);
                                Toast.makeText(MainActivity.this, "FOLDER: " + path, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build()
                        .show();
//                new MaterialFilePicker()
//                        .withActivity(MainActivity.this)
//                        .withRequestCode(1)
//                        .withFilter(Pattern.compile(".*\\.json$")) // Filtering files and directories by file name using regexp
//                        .withFilterDirectories(false) // Set directories filterable (false by default)
//                        .withHiddenFiles(true) // Show hidden files and folders
//                        .start();
            }
        });

        exportBtn = (Button) findViewById(R.id.export_button);
        exportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ArrayList<Integer> countList = new ArrayList<>();
                if (cardsList != null) {
                    for (int i = 0; i < cardsList.size(); i++) {
                        if (cardsList.get(i).isCheckbox()) {
                            countList.add(i);
                        }
                    }

                    if (countList.size() != 0) {
                        new ChooserDialog().with(MainActivity.this)
                                .withFilter(true, false)
                                .withStartFile("")
                                .withChosenListener(new ChooserDialog.Result() {
                                    @Override
                                    public void onChoosePath(String path, File pathFile) {
                                        // it takes a path and areas that you want to export
                                        MyJSON.exportFile(MainActivity.this, path, countList);
                                        Toast.makeText(MainActivity.this, "FOLDER: " + path, Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .build()
                                .show();
//                        Toast.makeText(MainActivity.this, "Έχετε επιλέξει " + count + " αντικείμενα", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Δέν έχετε επιλέξει κάποιο αντικείμενο", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void openDrawer() {

        areasList = MyJSON.getData(MainActivity.this);
        if (areasList.size() == 0) {
            Toast.makeText(MainActivity.this, "Δέν υπάρχουν διαθέσιμες περιοχές.", Toast.LENGTH_SHORT).show();
            return;
        }

//        Menu menu = navigationView.getMenu();
//        menu.removeGroup(0);
        cardsList = new ArrayList<>();

        if (!areasList.isEmpty()) {
            for (int i = 0; i < areasList.size(); i++) {
//                menu.add(0, i, i, areasList.get(i).getTitle());
                cardsList.add(new Card(areasList.get(i).getTitle()));
            }
        } else {
            Log.d(TAG, "openDrawer: areasList is empty");
        }

        adapter = new CustomListAdapter(cardsList, this);
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setAdapter(adapter);
//        navigationView.setNavigationItemSelectedListener(
//                new NavigationView.OnNavigationItemSelectedListener() {
//                    @Override
//                    public boolean onNavigationItemSelected(MenuItem menuItem) {
//                        menuItem.setChecked(true);
//                        if (new Permissions(MainActivity.this).isPermissionsOK()) {
//                            mDrawerLayout.closeDrawers();
//                            Intent intent = new Intent(MainActivity.this, LoadAreaMap.class);
//                            intent.putExtra("area", new Gson().toJson(areasList.get(menuItem.getItemId())));
//                            startActivity(intent);
//                        }
//                        return true;
//                    }
//                });
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int pos = mListView.getPositionForView(buttonView);
        if (pos != ListView.INVALID_POSITION) {
            Card c = cardsList.get(pos);
            c.setCheckbox(isChecked);

//            Toast.makeText(this, "CLicked Item: " + c.getTitle() + " state is: " + isChecked, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {
        int pos = mListView.getPositionForView(view);
        if (pos != ListView.INVALID_POSITION) {
            if (new Permissions(MainActivity.this).isPermissionsOK()) {
                mDrawerLayout.closeDrawers();
                Intent intent = new Intent(MainActivity.this, LoadAreaMap.class);
                intent.putExtra("area", new Gson().toJson(areasList.get(pos)));
                startActivity(intent);
            }
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == 1 && resultCode == RESULT_OK) {
//            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
//            Toast.makeText(this, "onActivityResult has been called", Toast.LENGTH_SHORT).show();
//            // Do anything with file
//        }
//    }
}
