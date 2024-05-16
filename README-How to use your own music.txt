#####Mod Info####
This mod allows you to mix and match custom music with vanilla music using LunaLib's options (startup menu screen, top left corner)

Different tracks will be played depending on your standing with the faction you are interacting with, and what you are doing to them (combat/visiting market/encountering fleets).

Only modifies what music to play, okay to add/remove mid-save.
Compatible with other music packs, but not mods that modify the music plugin(currently the only known one is Custom Battle Music).

Sample music included is generally more upbeat/less menacing than vanilla alternatives, also the Gacha track is used for surveying/looting, see if they suit you.

If you already know how to change music with sounds.json, what this mod adds is the option to
1 Customize combat music
2 Differentiate music for Luddic Church/Path (they share the same tracks in vanilla), also Player Faction, independent and [REDACTED] (all use the default faction music in vanilla).

Note that Sindrian Diktat is left vanilla by default because I couldn't find fitting HSR tracks for their custom use, but feel free to use some of your own. See the in-game instructions in Lunasettings on how to add your custom music.

Thanks to ruddygreat, Lukas04, Histidine and other members of the Galatia Academy for fixing my Java illiteracy.

For the sample music and logo, ©All rights reserved by miHoYo. Other properties and any right, title, and interest thereof and therein (intellectual property rights included) not derived from Honkai: Star Rail belong to their respective owners. Please do not use or redistribute any part of this mod commercially. Find Hoyo's guide on using their stuff here.

#####Using your own music####
For Players:
You can replace sample custom music in this mod with your own music (file must be in .ogg format) or add them into the shuffle.

To replace:
Simply navigate to the /Starsector/mods/Music Switcher/sounds folder, find whatever .ogg file you want to replace, delete it, rename your own .ogg file to what it used to be and paste in its place.

To add multiple tracks to a shuffle:
1 Do the same as above, but since only one file can use the old file's name, you need to rename the rest into for example MyMusicFile2.ogg
2 Open /Starsector/mods/Music Switcher/data/config/sounds.json with a notepad, Ctrl+F to search for the first file name, for example
   "CustomDiktatEncounterHostile.ogg",
and add
   "MyMusicFile2.ogg",
one line below it (inside the overarching "files":[   ] bracket)
Don't lose the comma at the end of each line, tweak the volume if you want to.

Restart the game to take effect.
If you fiddled with sounds.json and the game refuses to start and gives you an error message, you probably missed/duplicated a comma or a bracket.

For Modders:

You can add customized combat music and let Music Switcher play it when the player fights your mod's faction:

1 Add the following to your myfaction.faction file 
  "music": {"combat": "Custom_music_myfaction_combat"}
2 Add the following to your mod's sounds.json
  "music":
{
"Custom_music_myfaction_combat":
   [
      {
        "source":"sounds/Combat/",
        "volume":0.80,
        "file":"MyMusicFile2.ogg",
      }
   ]
}
3 Create a [sounds/Combat/] folder in your mod's base directory if you don't have one already, put MyMusicFile2.ogg inside.
4 Instruct the player to use Music Switcher if they want to hear custom music while fighting your faction. This would be a soft dependency, meaning that they can choose not to use Music Switcher, and other functions of your mod will be be unaffected.
