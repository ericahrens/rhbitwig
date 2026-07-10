package com.traktor.bridge;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;

public class VirtualMidiInput {
    private MidiDevice.Info[] deviceInfos;
    private MidiDevice inputDevice;
    private Transmitter transmitter;
    private String deviceName = "Traktor Bridge MIDI Input";
    private boolean isOpen = false;
    private List<String> availablePorts = new ArrayList<>();
    
    private Receiver currentReceiver;
    
    public VirtualMidiInput() {
        System.out.println("Initializing Virtual MIDI Input...");
        initializeDevice();
    }
    
    private void initializeDevice() {
        try {
            deviceInfos = MidiSystem.getMidiDeviceInfo();
            
            System.out.println("Available MIDI devices:");
            for (MidiDevice.Info info : deviceInfos) {
                System.out.println("  - " + info.getName() + " (Vendor: " + info.getVendor() + ")");
                availablePorts.add(info.getName());
            }
            
            String[] virtualPortNames = {
                "IAC Driver",
                "loopMIDI",
                "VirtualMIDI",
                "Virtual",
                "MIDI",
                "Bus",
                "Traktor",
                "Bridge"
            };
            
            for (MidiDevice.Info info : deviceInfos) {
                String name = info.getName();
                boolean isVirtual = false;
                for (String vName : virtualPortNames) {
                    if (name.contains(vName)) {
                        isVirtual = true;
                        break;
                    }
                }
                
                if (isVirtual) {
                    try {
                        MidiDevice device = MidiSystem.getMidiDevice(info);
                        if (device.getMaxTransmitters() != 0) {
                            inputDevice = device;
                            inputDevice.open();
                            transmitter = inputDevice.getTransmitter();
                            currentReceiver = inputDevice.getReceiver();
                            isOpen = true;
                            System.out.println("✓ Virtual MIDI input port opened: " + info.getName());
                            System.out.println("  Bitwig can now receive MIDI from this port");
                            return;
                        }
                    } catch (Exception e) {
                        System.out.println("  Cannot open: " + info.getName() + " - " + e.getMessage());
                    }
                }
            }
            
            System.out.println("No virtual MIDI input found. Trying to create one...");
            createVirtualInput();
            
        } catch (Exception e) {
            System.err.println("Error initializing MIDI: " + e.getMessage());
        }
    }
    
    private void createVirtualInput() {
        try {
            System.out.println("Trying to create virtual MIDI input...");
            
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxTransmitters() != 0 && !device.isOpen()) {
                    try {
                        device.open();
                        inputDevice = device;
                        transmitter = device.getTransmitter();
                        currentReceiver = device.getReceiver();
                        isOpen = true;
                        System.out.println("✓ Opened MIDI input port: " + info.getName());
                        System.out.println("  Bitwig can now receive MIDI from this port");
                        return;
                    } catch (Exception e) {
                    }
                }
            }
            
            System.out.println("⚠ Could not create virtual MIDI input.");
            System.out.println("  For Mac: Enable IAC Driver in Audio MIDI Setup");
            System.out.println("  For Windows: Install loopMIDI");
            System.out.println("  Then create a new port and make it available as INPUT");
            
        } catch (Exception e) {
            System.err.println("Failed to create virtual input: " + e.getMessage());
        }
    }
    
    public void sendMidi(int status, int data1, int data2) {
        if (currentReceiver == null || !isOpen) {
            sendMidiToAllInputs(status, data1, data2);
            return;
        }
        
        try {
            ShortMessage message = new ShortMessage();
            message.setMessage(status, data1, data2);
            currentReceiver.send(message, -1);
            System.out.println("✓ MIDI sent to virtual input: " + status + " " + data1 + " " + data2);
        } catch (Exception e) {
            System.err.println("Failed to send MIDI to input: " + e.getMessage());
            sendMidiToAllInputs(status, data1, data2);
        }
    }
    
    private void sendMidiToAllInputs(int status, int data1, int data2) {
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            int sentCount = 0;
            
            for (MidiDevice.Info info : infos) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    if (device.getMaxTransmitters() != 0) {
                        device.open();
                        Receiver recv = device.getReceiver();
                        ShortMessage msg = new ShortMessage();
                        msg.setMessage(status, data1, data2);
                        recv.send(msg, -1);
                        recv.close();
                        device.close();
                        sentCount++;
                        System.out.println("  Sent to: " + info.getName());
                    }
                } catch (Exception e) {
                }
            }
            
            if (sentCount > 0) {
                System.out.println("✓ MIDI sent to " + sentCount + " input(s)");
            } else {
                System.out.println("⚠ No MIDI input available to receive");
            }
            
        } catch (Exception e) {
            System.err.println("Fallback MIDI failed: " + e.getMessage());
        }
    }
    
    public void close() {
        if (currentReceiver != null) {
            try {
                currentReceiver.close();
            } catch (Exception e) {
            }
        }
        if (transmitter != null) {
            try {
                transmitter.close();
            } catch (Exception e) {
            }
        }
        if (inputDevice != null && inputDevice.isOpen()) {
            try {
                inputDevice.close();
            } catch (Exception e) {
            }
        }
        isOpen = false;
        System.out.println("Virtual MIDI input closed");
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public List<String> getAvailablePorts() {
        return availablePorts;
    }
}