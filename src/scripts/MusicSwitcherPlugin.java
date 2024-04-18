package scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import lunalib.lunaSettings.LunaSettings;

import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;

import java.util.Objects;
import java.util.Random;

public class MusicSwitcherPlugin extends MusicPlayerPluginImpl
{
    protected String CheckFactionid = null;
    protected String CheckMusicOption = null;
    protected Random coin = new Random();
    protected Boolean flip = coin.nextBoolean();

    protected String CombineFactionid(String Factionid)
    //Combines faction ids for grouped music options
    {
        switch (Factionid)
        {
            case "knights_of_ludd":
            case "luddic_church":
                Factionid = "luddic_church";
                break;
            case "lions_guard":
            case "sindrian_diktat":
                Factionid = "sindrian_diktat";
                break;
            case "player":
            case "hegemony":
            case "luddic_path":
            case "persean":
            case "remnant":
            case "omega":
            case "pirates":
            case "tritachyon":
                break;
            case "independent":
            case "derelict":
            case "scavengers":
            case "neutral":
            case "sleeper":
            case "poor":
                Factionid = "default";
                break;
            default:
                //mod faction
                Factionid = "other_faction";
                break;
        }
        return Factionid;
    }

    protected String SetMusic(String MusicName, String MusicOption)
    //For non-faction non-combat music
    {
        if (MusicName != null && !MusicName.equals("music_none"))
        {
            switch (MusicOption)
            {
                case "Custom":
                    return "Custom_" + MusicName;
                case "Both":
                    flip = coin.nextBoolean();
                    System.out.println("flip and setting " + MusicName);
                    return (flip) ? ("Custom_" + MusicName) : (MusicName);
                case "Vanilla":
                default:
                    //MusicOption null
                    System.out.println("default setting " + MusicName);
                    return MusicName;
            }
        }
        System.out.println("setting failed");
        return "music_none";
    }

    protected String SetFactionMusic(String VanillaMusic, String FactionMusicType, String MusicOption)
    {
        if (MusicOption != null)
        {
            System.out.println("option not null");
            if (!Objects.equals(MusicOption, "other_faction"))
            // not third party mod faction
            {
                System.out.println("not other_faction");
                switch (MusicOption)
                {
                    case "Custom":
                        FactionMusicType = "Custom_" + FactionMusicType;
                        System.out.println("vanilla faction music custom");
                        break;
                    case "Both":
                        flip = coin.nextBoolean();
                        FactionMusicType = (flip) ? FactionMusicType : "Custom_" + FactionMusicType;
                        System.out.println("vanilla faction flip");
                        break;
                    case "Vanilla":
                    default:
                        //MusicOption null, or somehow faction id didn't combine to it
                        break;
                }
            }
            else
            // third party mod faction
            {
                switch (MusicOption)
                {
                    case "Custom":
                        return "Custom_music_default_" + FactionMusicType;
                    case "Both":
                        flip = coin.nextBoolean();
                        System.out.println("other_faction flip");
                        if (VanillaMusic != null)
                        {return (flip) ? "Custom_music_default_" + FactionMusicType : VanillaMusic;}
                        else
                        {return (flip) ? "Custom_music_default_" + FactionMusicType : "music_default_" + FactionMusicType;}
                    case "Vanilla":
                    default:
                        //MusicOption for "other_faction" null, or somehow faction id didn't combine to it
                        break;
                }
            }
        }
        return null;
    }

    public String getMusicSetIdForCombat(CombatEngineAPI engine)
    {
        if (engine.isInCampaign())
        {
            String musicSetId = super.getMusicSetIdForCombat(engine);

            FactionAPI faction = engine.getContext().getOtherFleet().getFaction();
            String CheckFactionid = CombineFactionid(faction.getId());
            String CheckMusicOption = LunaSettings.getString("Music_Switcher", CheckFactionid + "_combat_Music");
            if (Objects.equals(CheckMusicOption, "Entire sample playlist"))
            {return "All_music_combat";}
            if (CheckMusicOption != null)
            {
                if (!Objects.equals(CheckFactionid, "other_faction"))
                //not third party mod faction
                {
                    switch (CheckMusicOption)
                    {
                        case "Faction Specific":
                            musicSetId = faction.getMusicMap().get("combat");
                            break;
                        case "Generic Combat Music":
                            musicSetId = "Custom_music_combat";
                            break;
                        case "Faction + Generic Shuffle":
                            flip = coin.nextBoolean();
                            musicSetId = (flip) ? "Custom_music_combat" : faction.getMusicMap().get("combat");
                            System.out.println("Combat flip");
                            break;
                        case "Entire Sample Playlist":
                            musicSetId = "All_music_combat";
                            break;
                        case "Vanilla":
                        default:
                            //MusicOption null, or somehow faction id didn't combine to it
                            break;
                    }
                }
                else
                // third party mod faction
                {
                    switch (CheckMusicOption)
                    {
                        case "Faction + Generic Shuffle":
                            flip = coin.nextBoolean();
                            if (faction.getMusicMap().get("combat") != null)
                            {musicSetId = (flip) ? "Custom_music_combat" : faction.getMusicMap().get("combat");}
                            else {musicSetId = "Custom_music_combat";}
                            break;
                        case "Generic Combat Music":
                            musicSetId = "Custom_music_combat";
                            break;
                        case "Faction Specific":
                            if (faction.getMusicMap().get("combat") != null)
                            {musicSetId = faction.getMusicMap().get("combat");}
                            else {musicSetId = "Custom_music_combat";}
                            break;
                        case "Entire Sample Playlist":
                            musicSetId = "All_music_combat";
                            break;
                        case "Vanilla":
                        default:
                            //MusicOption for "other_faction" null, or somehow faction id didn't combine to it
                            break;
                    }
                }
            }
            if (musicSetId != null)
            {
                System.out.println(musicSetId);
                return musicSetId;
            }
        }
        else if (!LunaSettings.getString("Music_Switcher", "default_combat_Music").equals("Vanilla"))
        //not in campaign, is in simulation or arcade
        {return "Custom_music_combat";}
        System.out.println("combat super");
        return super.getMusicSetIdForCombat(engine);
    }

    public String getMusicSetIdForTitle()
    {
        return SetMusic("music_title", LunaSettings.getString("Music_Switcher", "Menu_Music"));
    }

    protected String getPlanetSurveyMusicSetId(Object param)
    {
        return SetMusic("music_survey_and_scavenge", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
    }

    protected String getHyperspaceMusicSetId()
    {
        return SetMusic("music_campaign_hyperspace", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
    }

    protected String getStarSystemMusicSetId()

    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet.getContainingLocation() instanceof StarSystemAPI)
        {
            StarSystemAPI system = (StarSystemAPI) playerFleet.getContainingLocation();
            String musicSetId = system.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
            if (musicSetId != null) return musicSetId;

            if (system.hasTag(Tags.THEME_CORE) ||
                    !Misc.getMarketsInLocation(system, Factions.PLAYER).isEmpty())
            {
                return SetMusic("music_campaign", LunaSettings.getString("Music_Switcher", "Campaign_Music"));
            }
        }

        return SetMusic("music_campaign_non_core", LunaSettings.getString("Music_Switcher", "Campaign_Music"));
    }

    protected String getEncounterMusicSetId(Object param)
    {
        if (param instanceof SectorEntityToken)
        {
            SectorEntityToken token = (SectorEntityToken) param;

            String musicSetId = token.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
            if (musicSetId != null) return musicSetId;

            if (Entities.ABYSSAL_LIGHT.equals(token.getCustomEntityType()))
            {
                return SetMusic("music_encounter_neutral", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
            }
            if (Entities.CORONAL_TAP.equals(token.getCustomEntityType()))
            {
                return SetMusic("music_encounter_mysterious", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
            }
            if (Entities.WRECK.equals(token.getCustomEntityType()))
            {
                return SetMusic("music_encounter_neutral", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
            }
            if (Entities.DERELICT_GATEHAULER.equals(token.getCustomEntityType()))
            {
                return SetMusic("music_encounter_mysterious_non_aggressive", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
            }

            if (Entities.DEBRIS_FIELD_SHARED.equals(token.getCustomEntityType()))
            {
                return SetMusic("music_survey_and_scavenge", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
            }
            if (token.hasTag(Tags.GATE))
            {
                return SetMusic("music_encounter_neutral", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
            }
            if (token.hasTag(Tags.SALVAGEABLE))
            {
                if (token.getMemoryWithoutUpdate() != null && token.getMemoryWithoutUpdate().getBoolean("$hasDefenders"))
                {
                    if (token.getMemoryWithoutUpdate().getBoolean("$limboMiningStation"))
                    {
                        return SetMusic("music_encounter_mysterious", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
                    }
                    if (token.getMemoryWithoutUpdate().getBoolean("$limboWormholeCache"))
                    {
                        return SetMusic("music_encounter_mysterious", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
                    }
                    return SetMusic("music_encounter_neutral", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
                }
                return SetMusic("music_survey_and_scavenge", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
            }
            if (token.hasTag(Tags.SALVAGE_MUSIC))
            {
                return SetMusic("music_survey_and_scavenge", LunaSettings.getString("Music_Switcher", "Exploration_Music"));
            }

            if (token.getFaction() != null)
            {
                FactionAPI faction = (FactionAPI) token.getFaction();
                String type = null;
                //MemoryAPI mem = token.getMemoryWithoutUpdate();
                boolean hostile = false;
                boolean knowsWhoPlayerIs = false;
                if (token instanceof CampaignFleetAPI)
                {
                    CampaignFleetAPI fleet = (CampaignFleetAPI) token;
                    if (fleet.getAI() instanceof ModularFleetAIAPI)
                    {
                        hostile = ((ModularFleetAIAPI) fleet.getAI()).isHostileTo(Global.getSector().getPlayerFleet());
                    }
                    knowsWhoPlayerIs = fleet.knowsWhoPlayerIs();
                }

                if (faction.isAtWorst(Factions.PLAYER, RepLevel.FAVORABLE) && knowsWhoPlayerIs && !hostile)
                {
                    type = "encounter_friendly";
                }
                else if ((faction.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS) && knowsWhoPlayerIs) || hostile)
                {
                    type = "encounter_hostile";
                }
                else
                {
                    type = "encounter_neutral";
                }

                CheckFactionid = CombineFactionid(faction.getId());
                CheckMusicOption = LunaSettings.getString("Music_Switcher", CheckFactionid + "_Music");
                musicSetId = faction.getMusicMap().get(type);
                musicSetId = SetFactionMusic(musicSetId, type, CheckMusicOption);
                if (musicSetId != null)
                {return musicSetId;}
            }
        }
        System.out.println("encounter super");
        return super.getEncounterMusicSetId(param);
    }

    protected String getMarketMusicSetId(Object param)
    {
        if (param instanceof MarketAPI)
        {
            MarketAPI market = (MarketAPI) param;
            String musicSetId = market.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
            if (musicSetId != null) return musicSetId;

            if (market.getPrimaryEntity() != null &&
                    market.getPrimaryEntity().getMemoryWithoutUpdate().getBoolean("$abandonedStation"))
            {
                return getPlanetSurveyMusicSetId(param);
            }

            FactionAPI faction = market.getFaction();
            if (faction != null)
            {

                String type = null;
                if (faction.isAtWorst(Factions.PLAYER, RepLevel.FAVORABLE))
                {
                    type = "market_friendly";
                }
                else if (faction.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS))
                {
                    type = "market_hostile";
                }
                else
                {
                    type = "market_neutral";
                }

                CheckFactionid = CombineFactionid(faction.getId());
                CheckMusicOption = LunaSettings.getString("Music_Switcher", CheckFactionid + "_Music");
                musicSetId = faction.getMusicMap().get(type);
                musicSetId = SetFactionMusic(musicSetId, type, CheckMusicOption);
                if (musicSetId != null)
                {return musicSetId;}

            }
        }
        System.out.println("market super");
        return super.getMarketMusicSetId(param);
    }
}