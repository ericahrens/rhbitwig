# Specialized Bitwig Extensions for use with specific controllers on specific devices

* Novation Launchcontrol controls Bitwigs Arpeggiator Device
* Yaeltex SEQ ARP 168 controls Bitwigs Arpeggiator Device
* Launchpad Pro Mk3 Drum Sequencer/Editor
* Allen & Heath Xone:K2 specialized DJ Setup
* Akai Fire Drum Sequencer/Editor

* Akai Fire Copy & Paste - Correct Workflow
First tap → tap a pad to mark it as the source (it gets selected/highlighted)
Second tap → tap the same or another pad to copy its notes to the buffer (this loads the notes you want to paste)
Hold COPY button → keeps the buffer locked and ready for pasting
Tap any other pad (while holding COPY) → pastes the copied notes to that pad (executes twice automatically for proper playback)
Keep holding COPY → you can tap multiple pads one by one, and each will receive the same paste
Summary:
Select source → Copy to buffer → Hold COPY → Paste to destinations (multiple).

* Lexicon PSP42 ver. 2 is now also supported.
* For the connection between Tractor and Bitwig to work, you need to enable the IAC Driver from Audio MIDI Studio in the macOS settings, or use a virtual MIDI cable for Windows. In Bitwig’s controller settings, click “Add Controller” and select “TraktorBridge.”
Then, for the MIDI device, select the IAC Driver on Mac or another virtual MIDI cable.
 After completing the steps above, place the “Note Transpose” plugin before the synthesizer and adjust the semitones—to do this, don’t touch the controller; simply load a track into Traktor3/4 and use the deck’s output selection button, which will automatically send CC16 on channel 10 via the connection you’ve already set up. Once the mapping is done, you’re 100% ready for action!
 <img width="656" height="724" alt="a0867a65-5760-44ff-810a-2284b4b779dc" src="https://github.com/user-attachments/assets/538b25ff-6430-4f6e-85e3-8557541cbe3d" />
<img width="1000" height="180" alt="6d0a253f-e86b-47a8-952a-b67a1884cafd" src="https://github.com/user-attachments/assets/7cae2edc-6a1a-42c8-a612-d87c7bb8aa87" />
<img width="226" height="277" alt="K2_Key_Deck_Selector" src="https://github.com/user-attachments/assets/e066f9b5-a89b-4800-a02b-7214c88d2d88" />


