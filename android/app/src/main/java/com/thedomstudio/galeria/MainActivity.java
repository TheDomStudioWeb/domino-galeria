package com.thedomstudio.galeria;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(GaleriaPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
