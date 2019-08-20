package com.gmail.katsaros.s.dimitris.e_ktima;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MyJSON {

    private static String TAG = "MyJSON";

    private static final String FILE_NAME = "area_data.json";

    public static void saveData(Context context, String mJsonResponse) {
        try {
            FileWriter file = new FileWriter(context.getFilesDir().getPath() + "/" + FILE_NAME);
            file.write(mJsonResponse);
            file.flush();
            file.close();
        } catch (IOException e) {
            Log.e("TAG", "Error in Writing: " + e.getLocalizedMessage());
        }
    }

    public static void saveData(String mJsonResponse, String path, String fileName) {
        Log.d(TAG, "saveData2: has been called");
        try {
            FileWriter file = new FileWriter(path + "/" + fileName + ".json");
            file.write(mJsonResponse);
            file.flush();
            file.close();
        } catch (IOException e) {
            Log.e("TAG", "Error in Writing: " + e.getLocalizedMessage());
        }
    }

    public static ArrayList<AreaInfo> getData(Context context) {
        ArrayList<AreaInfo> list;
        String jsonString;
        try {
            File file = new File(context.getFilesDir().getPath() + "/" + FILE_NAME);
            FileInputStream is = new FileInputStream(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer);
            list = new Gson().fromJson(jsonString, new TypeToken<ArrayList<AreaInfo>>() {
            }.getType());

            return list;
        } catch (IOException e) {
            Log.e("TAG", "Error in Reading: " + e.getLocalizedMessage());
            saveData(context, "[]");
        } finally {
            try {
                File file = new File(context.getFilesDir().getPath() + "/" + FILE_NAME);
                FileInputStream is = new FileInputStream(file);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                jsonString = new String(buffer);
                list = new Gson().fromJson(jsonString, new TypeToken<ArrayList<AreaInfo>>() {
                }.getType());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("TAG", "Error in Reading: " + e.getLocalizedMessage());
                list = new ArrayList<>();
            }
            return list;
        }
    }

    public static AreaInfo getData(String path) {
        AreaInfo areaInfo;
        String jsonString;
        try {
            File file = new File(path);
            FileInputStream is = new FileInputStream(file);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer);
            areaInfo = new Gson().fromJson(jsonString, new TypeToken<AreaInfo>() {
            }.getType());

            return areaInfo;
        } catch (IOException e) {
            Log.e("TAG", "Error in Reading: " + e.getLocalizedMessage());
        } finally {
            try {
                File file = new File(path);
                FileInputStream is = new FileInputStream(file);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                jsonString = new String(buffer);
                areaInfo = new Gson().fromJson(jsonString, new TypeToken<AreaInfo>() {
                }.getType());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("TAG", "Error in Reading: " + e.getLocalizedMessage());
                areaInfo = new AreaInfo();
            }
            return areaInfo;
        }
    }

    public static void addArea(Context context, AreaInfo areaInfo) {

        ArrayList<AreaInfo> obj = getData(context);
        obj.add(areaInfo);

        String jsonString = new Gson().toJson(obj);
        saveData(context, jsonString);
        Toast.makeText(context, "Η περιοχή καταχωρήθηκε με επιτυχία", Toast.LENGTH_LONG).show();
    }

    public static void deleteArea(Context context, String id) {

        ArrayList<AreaInfo> obj = getData(context);
        for (int i = 0; i < obj.size(); i++) {
            if (id.equals(obj.get(i).getId())) {
                obj.remove(i);
                String jsonString = new Gson().toJson(obj);
                saveData(context, jsonString);
                return;
            }
        }
    }

    public static void clearData(Context context) {
        try {
            FileWriter file = new FileWriter(context.getFilesDir().getPath() + "/" + FILE_NAME);
            file.write("[]");
            file.flush();
            file.close();
        } catch (IOException e) {
            Log.e("TAG", "Error in Writing: " + e.getLocalizedMessage());
        }
    }

    public static void exportFile(Context context, String path, ArrayList<Integer> countList) {
        ArrayList<AreaInfo> data = getData(context);
        for (int i = 0; i < countList.size(); i++) {
            String jsonString = new Gson().toJson(data.get(countList.get(i)));
            saveData(jsonString, path, data.get(countList.get(i)).getTitle() + "_area_" + (i + 1));
        }
    }

    public static void importFile(Context context, String path) {
        AreaInfo area = getData(path);
        MyJSON.addArea(context, area);
    }
}