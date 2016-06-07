package com.immersion.videoplayer.haptics;

import android.content.Context;
import android.util.Log;

import com.immersion.uhl.Device;
import com.immersion.uhl.IVTBuffer;
import com.immersion.uhl.ImmVibe;
import com.immersion.uhl.Launcher;
import com.immersion.uhl.MagSweepEffectDefinition;
import com.immersion.uhl.PeriodicEffectDefinition;
import com.immersion.uhl.WaveformEffectDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Class responsible for loading HapticEffects from
 * and is also responsible for loading *.ivt file either from sdCard or from assets folder.
 * Currently, ivt file is loaded from assets and hapt from sdcard from given file location.
 * All events defined on Manifest will be passed on this class and then it calls
 * IVTBUffer and com.immersion.uhl.Device to play those effects
 */
public class HapticsManager {
    private static final String LOG_TAG = HapticsManager.class.getName();
    private static final boolean DEBUG = true;
    private static HapticsManager sInstance;

    private static final String INVALID_EFFECT_INDEX = "VIBE_E_INVALID_ARGUMENT";
    private static final String INVALID_EFFECT_NAME = "VIBE_E_FAIL";
    public static final String DEFAULT_FOLDER_ON_SDCARD = "hvuc/";
    public static final String DEFAULT_FOLDER_ON_SDCARD_HAPT = "hvuc/hapt/";
    public static final String DEFAULT_FOLDER_ON_SDCARD_IVT = "hvuc/ivt/";
    public static final String DEFAULT_FOLDER_IN_ASSETS = "haptics/";
    public static final String DEFAULT_IVT_FILENAME = "tactile_video_feed.ivt";
    public static final String DEFAULT_HAPT_FILENAME = "tactile_video_feed.hapt";

    public String ivtLocationFromXml = null;


    public static final String POSTIX_FOR_ASSETS_FILE = "";
    public static final String FULL_FILE_NAME_IN_ASSETS = "%s%s";
    private static final int DEFAULT_UHL_FALLBACK_EFFECT = Launcher.BOUNCE_66;


    private Context mContext;
    private static Device mDevice;
    private HashMap<String, IVTBuffer> mIVTBuffersMap;
    protected Launcher mUHLLauncher;
    private IVTBuffer mCurrentIVTBuffer;
    private boolean isMute;

    public static HapticsManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (HapticsManager.class) {
                if (sInstance == null) {
                    sInstance = new HapticsManager(context);
                }
            }
        }
        return sInstance;
    }

    public Device getDevice() {
        return mDevice;
    }

    public boolean isMute() {
        return isMute;
    }

    public void setMute(boolean mute) {
        isMute = mute;
        System.out.println("IsMute " + isMute);
        if (mute) {
            stop();
        }
    }

    public void playPeriodicEffect(PeriodicEffectDefinition effect) {
        if (!isMute())
            mDevice.playPeriodicEffect(effect);
    }

    public void playMagSweepEffect(MagSweepEffectDefinition effect) {
        if (!isMute())
            mDevice.playMagSweepEffect(effect);
    }

    private HapticsManager(Context context) {
        mContext = context;
        mIVTBuffersMap = new HashMap<String, IVTBuffer>();
        mDevice = Device.newDevice(context, 0);
        mUHLLauncher = new Launcher(context);
    }

    public void setIvtFileLocationFromXml(String ivtFileLocationFromXml) {
        this.ivtLocationFromXml = ivtFileLocationFromXml;
    }

    public void loadIVTFile(String filePath) throws FileNotFoundException {
        mCurrentIVTBuffer = null;
        IVTBuffer ivtBuffer;
        if (!mIVTBuffersMap.containsKey(filePath)) {
            ivtBuffer = loadIVTFileFromSdcardOrAssets(mContext, filePath);
            mIVTBuffersMap.put(filePath, ivtBuffer);
        } else {
            ivtBuffer = mIVTBuffersMap.get(filePath);
        }
        mCurrentIVTBuffer = ivtBuffer;
    }

    public IVTBuffer getCurrentIVTBuffer() {
        return mCurrentIVTBuffer;
    }

    public void playIvt(String filename, String ivtEffect, int defaultValue) {
        boolean needFallBack = false;
        if (DEBUG)
            Log.d(LOG_TAG, String.format("Trying to play %s effect from ivt", ivtEffect));
        try {
            if (mCurrentIVTBuffer == null) {
                throw new IllegalStateException("No buffers currently loaded. Use loadIVTFile()");
            }
            int effectIndex = 0;
            mCurrentIVTBuffer = mIVTBuffersMap.get(filename);
            try {
                effectIndex = mCurrentIVTBuffer.getEffectIndexFromName(ivtEffect);
            } catch (RuntimeException e) {
                if (e.getMessage().equals(INVALID_EFFECT_NAME)) {
                    throw new IllegalArgumentException("Cannot find effect with name " + ivtEffect);
                } else {
                    throw e;
                }
            }
            try {
                mDevice.playIVTEffect(mCurrentIVTBuffer, effectIndex);
            } catch (RuntimeException e) {
                if (e.getMessage().equals(INVALID_EFFECT_INDEX)) {
                    throw new IllegalArgumentException("Cannot find effect with id " + effectIndex);
                } else {
                    throw e;
                }
            }
        } catch (IllegalStateException e) {
            if (DEBUG)
                Log.e(LOG_TAG, e.getMessage());
            needFallBack = true;
        } catch (IllegalArgumentException e) {
            if (DEBUG)
                Log.e(LOG_TAG, e.getMessage());
            needFallBack = true;
        }
        if (needFallBack) {
            if (DEBUG)
                Log.d(LOG_TAG, String.format("Playing a file failed. Just play effect from UHL with id %d",
                        DEFAULT_UHL_FALLBACK_EFFECT));
        }
    }

    public void playIvt(String ivtEffect) {
        playFromIVT(ivtEffect, DEFAULT_UHL_FALLBACK_EFFECT);
    }

    public void playIvt(int defEffect) {
        if (!isMute)
            mUHLLauncher.play(defEffect);
    }

    private IVTBuffer loadIVTFileFromSdcardOrAssets(Context context, String fileName) throws FileNotFoundException {
        File sdCardFile;
        //First preference from file
        sdCardFile = new File(ivtLocationFromXml);
        IVTBuffer ivtBuffer = null;

        if (DEBUG)
            Log.d(LOG_TAG, "Trying to load ivt file from Xml Given SDCard: " + sdCardFile.getAbsolutePath());
        if (sdCardFile.exists()) {
            try {
                ivtBuffer = createIVTBufferFromFile(sdCardFile);
            } catch (IOException e) {
                e.printStackTrace();
                if (DEBUG)
                    Log.w(LOG_TAG, "Cannot load file from Xml given sdcard: " + e.getMessage());
            }
        }


        //second preference from default sdcard location
        if (ivtBuffer == null) {
//            sdCardFile = new File(Environment.getExternalStorageDirectory(), DEFAULT_FOLDER_ON_SDCARD_IVT + fileName);
            sdCardFile = new File(getExternalDir(), DEFAULT_FOLDER_ON_SDCARD_IVT + fileName);

            if (DEBUG)
                Log.d(LOG_TAG, "Trying to load ivt file from SDCard: " + sdCardFile.getAbsolutePath());
            if (sdCardFile.exists()) {
                try {
                    ivtBuffer = createIVTBufferFromFile(sdCardFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    if (DEBUG)
                        Log.w(LOG_TAG, "Cannot load file from sdcard: " + e.getMessage());
                }
            }
        }

        String fileNameInAssets = "";
        if (ivtBuffer == null) {
            fileNameInAssets = String.format(FULL_FILE_NAME_IN_ASSETS, DEFAULT_FOLDER_IN_ASSETS, fileName);
            if (DEBUG)
                Log.d(LOG_TAG, "Trying to load " + fileNameInAssets + " from assets");
            try {
                InputStream is = context.getAssets().open(fileNameInAssets);
                ivtBuffer = createIVTBufferFromInputStream(is);
            } catch (IOException e) {
                e.printStackTrace();
                if (DEBUG)
                    Log.w(LOG_TAG, "Cannot load file from assets: " + e.getMessage());
            }
        }

        if (ivtBuffer == null) {
            throw new FileNotFoundException(String.format("File with name %s not found neither in %s, neither in asseets folder with name %s",
                    fileName, sdCardFile.getAbsolutePath(), fileNameInAssets));
        }

        return ivtBuffer;
    }

    private IVTBuffer createIVTBufferFromInputStream(InputStream is) throws IOException {
        long length = is.available();

        if (length > Integer.MAX_VALUE) {
            throw new IOException("File is too large!");
        }

        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;

        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file");
        }

        is.close();

        return new IVTBuffer(bytes);
    }

    protected IVTBuffer createIVTBufferFromFile(File ivtFile) throws IOException {
        InputStream in = new FileInputStream(ivtFile);
        return createIVTBufferFromInputStream(in);
    }

    public void playRepeatedFromIVT(String name, int uhlFallBackEffect) {
        boolean needFallBack = false;
        if (DEBUG)
            Log.d(LOG_TAG, String.format("Trying to play %s effect from ivt", name));
        try {
            if (mCurrentIVTBuffer == null) {
                throw new IllegalStateException("No buffers currently loaded. Use loadIVTFile()");
            }
            int effectIndex = 0;
            for (Map.Entry<String, IVTBuffer> entry : mIVTBuffersMap.entrySet()) {
                if (entry.getKey().contains(name)) {
                    mCurrentIVTBuffer = entry.getValue();
                }
            }

            try {
                effectIndex = mCurrentIVTBuffer.getEffectIndexFromName(name);
            } catch (RuntimeException e) {
                if (e.getMessage().equals(INVALID_EFFECT_NAME)) {
                    throw new IllegalArgumentException("Cannot find effect with name " + name);
                } else {
                    throw e;
                }
            }
            try {
                if (!isMute)
                    mDevice.playIVTEffectRepeat(mCurrentIVTBuffer, effectIndex, (byte) ImmVibe.VIBE_REPEAT_COUNT_INFINITE);
            } catch (RuntimeException e) {
                if (e.getMessage().equals(INVALID_EFFECT_INDEX)) {
                    throw new IllegalArgumentException("Cannot find effect with id " + effectIndex);
                } else {
                    throw e;
                }
            }
        } catch (IllegalStateException e) {
            if (DEBUG)
                Log.e(LOG_TAG, e.getMessage());
            needFallBack = true;
        } catch (IllegalArgumentException e) {
            if (DEBUG)
                Log.e(LOG_TAG, e.getMessage());
            needFallBack = true;
        }
        if (needFallBack) {
            if (DEBUG)
                Log.d(LOG_TAG, String.format("Playing a file failed. Just play effect from UHL with id %d",
                        uhlFallBackEffect));
        }
    }

    public void playFromIVT(String name, int uhlFallBackEffect) {
        boolean needFallBack = false;
        if (DEBUG)
            Log.d(LOG_TAG, String.format("Trying to play %s effect from ivt", name));
        try {
            if (mCurrentIVTBuffer == null) {
                throw new IllegalStateException("No buffers currently loaded. Use loadIVTFile()");
            }
            int effectIndex = 0;
            for (Map.Entry<String, IVTBuffer> entry : mIVTBuffersMap.entrySet()) {
                if (entry.getKey().contains(name)) {
                    mCurrentIVTBuffer = entry.getValue();
                }
            }

            try {
                effectIndex = mCurrentIVTBuffer.getEffectIndexFromName(name);
            } catch (RuntimeException e) {
                if (e.getMessage().equals(INVALID_EFFECT_NAME)) {
                    throw new IllegalArgumentException("Cannot find effect with name " + name);
                } else {
                    throw e;
                }
            }
            try {
                if (!isMute)
                    mDevice.playIVTEffect(mCurrentIVTBuffer, effectIndex);
            } catch (RuntimeException e) {
                if (e.getMessage().equals(INVALID_EFFECT_INDEX)) {
                    throw new IllegalArgumentException("Cannot find effect with id " + effectIndex);
                } else {
                    throw e;
                }
            }
        } catch (IllegalStateException e) {
            if (DEBUG)
                Log.e(LOG_TAG, e.getMessage());
            needFallBack = true;
        } catch (IllegalArgumentException e) {
            if (DEBUG)
                Log.e(LOG_TAG, e.getMessage());
            needFallBack = true;
        }
        if (needFallBack) {
            if (DEBUG)
                Log.d(LOG_TAG, String.format("Playing a file failed. Just play effect from UHL with id %d",
                        uhlFallBackEffect));
        }
    }


    public void stop() {
        mUHLLauncher.stop();
        mDevice.stopAllPlayingEffects();
    }

    public void playWaveformEffect(WaveformEffectDefinition wed) {
        if (!isMute())
            mDevice.playWaveformEffect(wed);
    }

    public void playFromIVT(int index, int uhlFallBackEffect) {
        boolean needFallBack = false;
        try {
            if (mCurrentIVTBuffer == null) {
                if (DEBUG)
                    throw new IllegalStateException("No buffers currently loaded. Use loadIVTFile()");
            }
            try {
                if (!isMute)
                    mDevice.playIVTEffect(mCurrentIVTBuffer, index);
            } catch (RuntimeException e) {
                if (e.getMessage().equals(INVALID_EFFECT_INDEX)) {
                    if (DEBUG)
                        throw new IllegalArgumentException("Cannot find effect with id " + index);
                }
            }
        } catch (IllegalStateException e) {
            needFallBack = true;
        } catch (IllegalArgumentException e) {
            needFallBack = true;
        }
        if (needFallBack) {
        }
    }

    private File getExternalDir() {
        return mContext.getExternalFilesDir(null);
    }

}
