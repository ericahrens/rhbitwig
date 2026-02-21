package com.yaeltex.djm;

public enum ProjectPage implements PageIdent {
    AUX_SENDS("AUX SENDS 1-2"),
    FLANGE_EFX("FLANGE & EFX"),
    GAIN_KNOBS("GAIN KNOBS"),
    BOOTH_EQ_MASTER_FILTER("BOOTH EQ & MASTER FILTER"),
    CUE_FILTER("CUE & FILTER"),
    CHANNEL_FADERS("CHANNEL FADERS"),
    FLG_FLT_SMALL("FLG FLT SMALL"),
    SMALL_FADERS("SMALL FADERS"),
    CHANNEL_EQ_A_B("CHANNEL EQ A-B"),
    CHANNEL_EQ_C_D("CHANNEL EQ C-D"),
    AUX2_PRE_POST("AUX2 PRE/POST & EQ-LF-EQ-CT Buttons"),
    STEMS_FX_BUTTON("STEMS FX / 2RED BT/ DIM/ FLT"),
    STEMS_A_B("STEMS ENCODERS A-B"),
    STEMS_C_D("STEMS ENCODERS C-D"),
    EQ_CT_BUTTONS("EQ / CT");
    
    private final String name;
    
    ProjectPage(final String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
}
