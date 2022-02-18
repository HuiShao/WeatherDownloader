/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weatherdownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.nio.file.*;

/**
 *
 * @author Shawn
 */
public class WeatherDownLoader {

    /**
     * FROM ENVIRONMENT CANADA
     * ftp://ftp.tor.ec.gc.ca/Pub/Get_More_Data_Plus_de_donnees/Readme.txt
     *
     * URL based procedure to automatically download data in bulk from Climate
     * Website (http://www.climate.weather.gc.ca) Version: 2016-05-10
     *
     * ---------------------------------------- ENVIRONMENT AND CLIMATE CHANGE
     * CANADA
     *
     *
     * To read this file online, please visit:
     * ftp://client_climate@ftp.tor.ec.gc.ca/Pub/Get_More_Data_Plus_de_donnees/
     * * Folder: Get_More_Data_Plus_de_donnees > Readme.txt
     *
     * Instructions on how to download all weather data for one station from
     * Environment and Climate Change Canada's Climate website: * A daily
     * updated list of Climate stations in the National Archive, including their
     * Climate ID, Station ID, WMO ID, TC ID, and co-ordinates can be found in
     * the following folder: Get_More_Data_Plus_de_donnees > Station Inventory
     * EN.csv
     *
     * Use the following utility to download data: • wget (GNU / Linux Operating
     * systems) • Cygwin (Windows Operating systems) https://www.cygwin.com •
     * Homebrew (OS X - Apple) http://brew.sh/ Example to download all available
     * hourly data for Yellowknife A, from 1998 to 2008, in .csv format
     *
     *
     * Command line: * for year in `seq 1998 2008`;do for month in `seq 1 12`;do
     * wget --content-disposition
     * "http://climate.weather.gc.ca/climate_data/bulk_data_e.html?format=csv&stationID=1706&Year=${year}&Month=${month}&Day=14&timeframe=1&submit=
     * Download+Data" ;done;done
     *
     *
     * WHERE; • year = change values in command line (`seq 1998 2008) • month =
     * change values in command line (`seq 1 12) • format= [csv|xml]: the format
     * output • timeframe = 1: for hourly data • timeframe = 2: for daily data •
     * timeframe = 3 for monthly data • Day: the value of the "day" variable is
     * not used and can be an arbitrary value • For another station, change the
     * value of the variable stationID • For the data in XML format, change the
     * value of the variable format to xml in the URL. * For information in
     * French, change Download+Data with
     * ++T%C3%A9l%C3%A9charger+%0D%0Ades+donn%C3%A9es, also change _e with _f in
     * the url.
     *
     *
     * For questions or concerns please contact our National Climate Services
     * office at: ec.services.climatiques-climate.services.ec@canada.ca *
     */
    private static void downloadClimateDataFromEnvironmentCanada(String outPath, String combinePath,
            int stationID, int year, int month, int day, int timeFrame) {
        FileOutputStream fos = null;
        try {
            File f = new File(outPath);
            if (!f.exists()) {
                try {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            fos = new FileOutputStream(new File(outPath));
            try {
                String url = String.format("https://climate.weather.gc.ca/climate_data/bulk_data_e.html?format=csv&stationID=%d&Year=%d&Month=%d&Day=%d&timeframe=%d&submit=Download+Data",
                        stationID,
                        year,
                        month,
                        day,
                        timeFrame
                );
                URL website = new URL(url);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (MalformedURLException ex) {
                Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fos.close();
                } catch (IOException ex) {
                    Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            FileInputStream fstream = new FileInputStream(outPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            boolean flag = false;
            StringBuilder sb = new StringBuilder();
            String sep = System.getProperty("line.separator");
            boolean isCombinePathExists = new File(combinePath).exists();
            
            try {
                while ((strLine = br.readLine()) != null) {
                    if (flag || !isCombinePathExists) {
                        sb.append(strLine);
                        sb.append(sep);
                    } else {
                        flag = strLine.contains("Longitude (x)");
                    }
                }

                PrintWriter outPW = new PrintWriter(new FileOutputStream(new File(combinePath), true));

                //output to the exported file
                outPW.append(sb.toString());

                //close the file (VERY IMPORTANT!)
                outPW.close();
            } catch (IOException ex) {
                Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void downloadClimateDataFromNASAPowerSinglePointV1(String outPath,
            String parameters,
            String startDate, String endDate, float latitude, float longitude) {
        String csvUrl = "";

        // download to json file to find the csv path        
        try {
            String url = String.format("https://power.larc.nasa.gov/cgi-bin/v1/DataAccess.py?request=execute&identifier=SinglePoint&parameters=%s&startDate=%s&endDate=%s&userCommunity=SSE&tempAverage=DAILY&outputList=CSV&lat=%.6f&lon=%.6f&user=anonymous",
                    parameters,
                    startDate,
                    endDate,
                    latitude,
                    longitude                                        
            );
            URL website = new URL(url);
            URLConnection request = website.openConnection();
            request.connect();

            // parsing file "JSONExample.json" 
            Object obj = new JSONParser().parse(new InputStreamReader((InputStream) request.getContent()));

            // typecasting obj to JSONObject 
            JSONObject jo = (JSONObject) obj;

            // getting address 
            Map address = ((Map) jo.get("parameters"));

            // iterating address Map 
            Iterator<Map.Entry> itr1 = address.entrySet().iterator();
            while (itr1.hasNext()) {
                Map.Entry pair = itr1.next();
                if (pair.getKey().toString().equalsIgnoreCase("csv")) {
                    csvUrl = pair.getValue().toString();
                    break;
                }
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

        // download csv
        try {
            File f = new File(outPath);
            if (!f.exists()) {
                try {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            FileOutputStream fos = new FileOutputStream(new File(outPath));
            try {
                URL website = new URL(csvUrl);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (MalformedURLException ex) {
                Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fos.close();
                } catch (IOException ex) {
                    Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    private static void downloadClimateDataFromNASAPowerSinglePointV2(String outPath,
            String parameters,
            String startDate, String endDate, float latitude, float longitude) {
        FileOutputStream fos = null;
        try {
            File f = new File(outPath);
            if (!f.exists()) {
                try {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            fos = new FileOutputStream(new File(outPath));
            try {
                String url = String.format("https://power.larc.nasa.gov/api/temporal/daily/point?parameters=%s&community=RE&longitude=%.6f&latitude=%.6f&start=%s&end=%s&format=CSV",
                        parameters,
                        longitude,
                        latitude,
                        startDate,
                        endDate
                );
                URL website = new URL(url);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (MalformedURLException ex) {
                Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fos.close();
                } catch (IOException ex) {
                    Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(WeatherDownLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void DownloadClimateDataFromEnvironmentCanada(String outDir, int month, int day, int startYr, int endYr, int timeFrame, Map<String, Integer> stations) {
        stations.entrySet().parallelStream().forEach((station) -> {
                for (int year = startYr; year <= endYr; year++) {
                    String outPath = String.format("%s%s\\%s_%d.csv", outDir, station.getKey(), station.getKey(), year);
                    String combinePath = String.format("%s%s\\%s.csv", outDir, station.getKey(), station.getKey());
                    WeatherDownLoader.downloadClimateDataFromEnvironmentCanada(outPath, combinePath, station.getValue(), year, month, day, timeFrame);
                }
            });
    }

    public static void DownloadClimateDataFromNASAPowerSinglePoint(String outDir,
            String parameters,
            String startDate, String endDate, Map<String, location> stations) {
        
        stations.entrySet().parallelStream().forEach((station) -> {
                String outPath = String.format("%s%s.csv", outDir, station.getKey());
                WeatherDownLoader.downloadClimateDataFromNASAPowerSinglePointV2(outPath, parameters, startDate, endDate, station.getValue().latitude, station.getValue().longitude);
            });
    }

    private static class location {

        float latitude;
        float longitude;

        public location(float lat, float lon) {
            latitude = lat;
            longitude = lon;
        }
    }
    
    public static void DownloadClimateDataFromEnvironmentCanada() {
        // Download data from ECCC
        String outDir = "C:\\Users\\shaoh\\Desktop\\EC_climate\\";
        int month = 1;
        int day = 1;
        int timeFrame = 2;
        int startYr = 1970;
        int endYr = 2021;

        Map<String, Integer> stations = new HashMap();
        stations.put("GODERICH MUNICIPAL A", 4567);
        stations.put("BRUCEFIELD", 4547);
        stations.put("SALTFORD", 4587);
        stations.put("DASHWOOD", 4557);
        stations.put("EXETER", 4560);
        stations.put("BLYTH", 4545);
        stations.put("GODERICH", 7747);
        
        WeatherDownLoader.DownloadClimateDataFromEnvironmentCanada(outDir, month, day, startYr, endYr, timeFrame, stations);
    }
    
    public static void DownloadClimateDataFromNASAPowerSinglePoint(){
        String outDir = "C:\\Users\\shaoh\\Desktop\\NASA\\";
        String startDate = "19810101";
        String endDate = "20211231";
        String parameters = "WS10M,RH2M,ALLSKY_SFC_SW_DWN";

        Map<String, location> stations = new HashMap();
        stations.put("GODERICH MUNICIPAL A", new location(43.77f, -81.7f));
        stations.put("BRUCEFIELD", new location(43.55f, -81.55f));
        stations.put("SALTFORD", new location(43.75f, -81.68f));
        stations.put("DASHWOOD", new location(43.37f, -81.62f));
        stations.put("EXETER", new location(43.35f, -81.5f));
        stations.put("BLYTH", new location(43.72f, -81.38f));
        stations.put("GODERICH", new location(43.77f, -81.72f));

        WeatherDownLoader.DownloadClimateDataFromNASAPowerSinglePoint(outDir, parameters, startDate, endDate, stations);
    }

    public static void main(String args[]) throws IOException {
        
        // Download climate data from ECCC
        DownloadClimateDataFromEnvironmentCanada();

        // Download from NASA for rolar radiation and relative humidity
        DownloadClimateDataFromNASAPowerSinglePoint();
    }

}
