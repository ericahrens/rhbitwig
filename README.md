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

<img width="1000" height="180" alt="6d0a253f-e86b-47a8-952a-b67a1884cafd" src="https://github.com/user-attachments/assets/7cae2edc-6a1a-42c8-a612-d87c7bb8aa87" />
<img width="656" height="724" alt="a0867a65-5760-44ff-810a-2284b4b779dc" src="https://github.com/user-attachments/assets/538b25ff-6430-4f6e-85e3-8557541cbe3d" />
<img width="226" height="277" alt="K2_Key_Deck_Selector" src="https://github.com/user-attachments/assets/e066f9b5-a89b-4800-a02b-7214c88d2d88" />

* Added support for both Allen & Heath XONE:K2 and XONE:K3 under the same controller script, with K3 auto-detection enabled. The extension label was updated to Allen & Heath K2/K3 DJSet, while keeping the existing MIDI mapping unchanged. The Traktor bridge was cleaned up, Camelot key handling was corrected, and Bitwig semitone CC output was scaled to match the -48..+48 parameter range.

* Report Update (K2 Deck Selector)
The 4 deck selector buttons now follow a clear interaction model:
- Single click on a non-active deck: temporary preview (red LED), no active deck change.
- Double click within 320 ms on the same deck: selects the deck.
- Press on the already selected deck: keeps selection and clears temporary state.
- Selected deck LED: yellow.
- When a deck is selected, K2 notifies the bridge via `POST /activeDeck/{A|B|C|D}`.
Tonality and Note Transpose mapping details are documented in the section below.

* Camelot -> Traktor Key -> Bitwig Note Transpose Mapping
Mapping pipeline used by the bridge:

`Traktor JSON key` (`resultingKey` / `key_text` / `key` / `track_key`)
-> normalize Camelot notation (`1A` -> `1m`, `8B` -> `8d`)
-> Camelot-to-note lookup (MIDI note)
-> `semitone = midiNote - 60` (C4 reference)
-> `CC16` value for Bitwig Note Transpose

Current CC conversion formula:

`CC16 = ceil(((semitone + 48) / 96) * 127)`

Verified mapping for Note Transpose range `-1 .. 10`:

| Note Transpose (semitone) | CC16 |
| --- | --- |
| -1 | 63 |
| 0 | 64 |
| 1 | 65 |
| 2 | 67 |
| 3 | 68 |
| 4 | 69 |
| 5 | 71 |
| 6 | 72 |
| 7 | 73 |
| 8 | 75 |
| 9 | 76 |
| 10 | 77 |

Camelot examples for the same semitone range:

| Semitone | Example Camelot keys |
| --- | --- |
| -1 | `6d`, `3m` |
| 0 | `1d`, `10m` |
| 1 | `8d`, `5m` |
| 2 | `3d`, `12m` |
| 3 | `10d`, `7m` |
| 4 | `5d`, `2m` |
| 5 | `12d`, `9m` |
| 6 | `7d`, `4m` |
| 7 | `2d`, `11m` |
| 8 | `9d`, `6m` |
| 9 | `4d`, `1m` |
| 10 | `11d`, `8m` |

Canonical wheel labels used in this project (source of truth):

| Camelot | Label |
| --- | --- |
| 12d | F |
| 12m | Dm |
| 1d | C |
| 1m | Am |
| 2d | G |
| 2m | Em |
| 3d | D |
| 3m | Bm |
| 4d | A |
| 4m | F#m/Gbm |
| 5d | E |
| 5m | C#m/Dbm |
| 6d | B |
| 6m | G#m/Abm |
| 7d | F# |
| 7m | D#m/Ebm |
| 8d | C# |
| 8m | A#m/Bdm |
| 9d | G# |
| 9m | Fm |
| 10d | D# |
| 10m | Cm |
| 11d | A# |
| 11m | Gm |

 | (C) | -> | 1d , 10m |
 | (C#) | -> | 8d , 5m |
 | (D) | -> | 3d , 12m |
 | (D#) | -> | 10d , 7m |
 | (E) | -> | 5d , 2m |
 | (F) | -> | 12d , 9m |
 | (F#) | -> | 7d , 4m |
 | (G) | -> | 2d , 11m |
 | (G#) | -> | 9d , 6m |
 | (A) | -> | 4d , 1m |
 | (A#) | -> | 11d , 8m |
 | (B) | -> | 6d , 3m |

* Fork Update vs Original `ericahrens/rhbitwig` (`upstream/master`)
Important differences in this forked branch:

1. New Traktor Bridge subsystem (not present in upstream master):
Added `com.traktor.bridge` package with `TraktorBitwigBridgeExtension`, `TraktorBitwigBridgeExtensionDefinition`, `HttpBridgeServer`, `DeckManager`, `ScaleEngine`, and `VirtualMidiInput`; this provides HTTP deck/key intake and MIDI/CC forwarding to Bitwig.

2. Allen & Heath K2/K3 workflow updates:
Extension renamed and aligned to K2/K3 DJSet; deck selection behavior stabilized (single-click preview, double-click select), active-deck notifications sent to bridge (`/activeDeck/{A|B|C|D}`), and LED state feedback refined.

3. Tonality/transpose pipeline hardening:
Camelot/Open-wheel mapping synchronized to project wheel definitions, semitone -> CC16 conversion adjusted to avoid skipped Note Transpose steps, and practical range `-1..10` verified without gaps.

4. API/toolchain alignment:
Maven Bitwig dependency aligned to `com.bitwig:extension-api:25`, `getRequiredAPIVersion()` aligned to 25 across extension definitions, and IntelliJ module metadata aligned to API 25.

5. Controller/framework evolution beyond bridge work:
Substantial updates in Akai Fire sequencing/display pipeline, Launchcontrol and Launchpad Pro Mk3 code paths, Yaeltex focus/layer logic refinements, and framework/debug cleanup and stabilization.

6. Documentation expanded in this fork:
Added K2 deck-selector behavior notes, full Camelot -> Note Transpose mapping documentation, and a canonical wheel table as source of truth.

