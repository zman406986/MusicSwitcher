

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
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.util.Misc;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import com.fs.starfarer.api.impl.campaign.tutorial.TutorialMissionIntel;

import java.util.*;

public class MusicSwitcherPlugin extends MusicPlayerPluginImpl
{

    private static final String PREFIX_CUSTOM = "Custom_";
    private static final String PREFIX_ALL = "All_";

    private static final Random RNG = new Random();

    private enum MusicOption
    {
        CUSTOM("Custom"), BOTH("Both"), VANILLA("Vanilla"), FACTION_SPECIFIC("Faction Specific"), GENERIC_COMBAT("Generic Combat Music"), FACTION_GENERIC_SHUFFLE("Faction + Generic Shuffle"), ENTIRE_SAMPLE("Entire Sample Playlist");
        private final String id;
        MusicOption(String id) {this.id = id;}
        static MusicOption of(String raw)
        {
            if (raw == null || raw.trim().isEmpty()) return VANILLA;
            for (MusicOption o : values()) if (o.id.equals(raw)) return o;
            return VANILLA;
        }
    }

    private final Map<String, String> settingsCache = new HashMap<>();

    private String getSetting(String key)
    {
        return settingsCache.computeIfAbsent(key, k -> Optional.ofNullable(LunaSettings.getString("Music_Switcher", k)).orElse("Vanilla"));
    }

    private static final Logger LOG = Global.getLogger(MusicSwitcherPlugin.class);
    private static boolean MASTER_LOGGING = false;
    static {MASTER_LOGGING = "YES".equalsIgnoreCase(LunaSettings.getString("Music_Switcher", "Log_Enabled"));}

    private static final Map<String, String> DEFAULT_CUSTOM_MUSIC_MAP;
    static
    {
        Map<String, String> tmp = new HashMap<>();
        tmp.put("Custom_market_neutral", "Custom_music_default_market_neutral");
        tmp.put("Custom_market_hostile", "Custom_music_default_market_hostile");
        tmp.put("Custom_market_friendly", "Custom_music_default_market_friendly");
        tmp.put("Custom_encounter_neutral", "Custom_music_default_encounter_neutral");
        tmp.put("Custom_encounter_hostile", "Custom_music_default_encounter_hostile");
        tmp.put("Custom_encounter_friendly", "Custom_music_default_encounter_friendly");
        DEFAULT_CUSTOM_MUSIC_MAP = Collections.unmodifiableMap(tmp);
    }

    private static final Map<String, String> FACTION_ALIAS;
    // These only combines names for option choices, so the user needs to select fewer options in lunaoptions. Does not actually change faction names for calling,
    static
    {
        Map<String, String> tmp = new HashMap<>();
        tmp.put("knights_of_ludd", "luddic_church");
        tmp.put("lions_guard", "sindrian_diktat");
        tmp.put("remnant", "redacted");
        tmp.put("omega", "redacted");
        tmp.put("dweller", "redacted");
        tmp.put("threat", "redacted");

        tmp.put("player", "player");
        tmp.put("hegemony", "hegemony");
        tmp.put("luddic_path", "luddic_path");
        tmp.put("persean", "persean");
        tmp.put("pirates", "pirates");
        tmp.put("tritachyon", "tritachyon");

        tmp.put("independent", "default");
        tmp.put("derelict", "default");
        tmp.put("scavengers", "default");
        tmp.put("neutral", "default");
        tmp.put("sleeper", "default");
        tmp.put("poor", "default");
        FACTION_ALIAS = Collections.unmodifiableMap(tmp);
    }

    private String canonicalFactionId(String id)
    {
        return FACTION_ALIAS.getOrDefault(id, "other_faction");
    }

    @Override
    public String getMusicSetIdForCombat(CombatEngineAPI engine)
    {
        if (engine == null || engine.getContext() == null || engine.getContext().getOtherFleet() == null)
        {
            if (MASTER_LOGGING) LOG.info("early frames null prevention activated");
            return super.getMusicSetIdForCombat(engine);
        }// early frames null prevention
        CampaignFleetAPI otherFleet = engine.getContext().getOtherFleet();
        FactionAPI otherFaction = otherFleet.getFaction();
        String factionId = otherFaction != null ? otherFaction.getId() : "none";
        MusicOption option = (otherFaction != null) ? MusicOption.of(getSetting(canonicalFactionId(factionId) + "_combat_Music")) : MusicOption.VANILLA;

        if (MASTER_LOGGING)
            LOG.info(String.format("Combat music check for faction '%s'. Option: '%s'", factionId, option.id));

        switch (option)
        {
            case GENERIC_COMBAT:
                return PREFIX_CUSTOM + "music_combat"; // Music Switcher's generic combat music
            case ENTIRE_SAMPLE:
                return PREFIX_ALL + "music_combat"; // Music Switcher's "all samples" combat music
            case FACTION_SPECIFIC:
                return safeGetMusic(otherFaction, "Combat", PREFIX_CUSTOM + "music_combat", option);
            case FACTION_GENERIC_SHUFFLE:
                if (RNG.nextBoolean() || otherFaction == null) return PREFIX_CUSTOM + "music_combat";
                else return safeGetMusic(otherFaction, "Combat", PREFIX_CUSTOM + "music_combat", option);
            case VANILLA:
            default:
                return super.getMusicSetIdForCombat(engine);
        }
    }

    @Override
    public String getMusicSetIdForTitle()
    {
        String result = transform("music_title", MusicOption.of(getSetting("Menu_Music")), PREFIX_CUSTOM);
        if (MASTER_LOGGING) LOG.info("Title music: " + result);
        return result;
    }

    @Override
    protected String getHyperspaceMusicSetId()
    {
        String result = transform("music_campaign_hyperspace", MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);
        if (MASTER_LOGGING) LOG.info("Hyperspace music: " + result);
        return result;
    }

    @Override
    protected String getPlanetSurveyMusicSetId(Object param)
    {
        SectorEntityToken token = null;
        if (param instanceof SectorEntityToken)
        {
            token = (SectorEntityToken) param;
        }
        else if (param instanceof MarketAPI)
        {
            token = ((MarketAPI) param).getPlanetEntity();
        }
        if (token != null)
        {
            String musicSetId = token.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY_MISSION);
            if (musicSetId != null) return musicSetId;
            musicSetId = token.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
            if (musicSetId != null) return musicSetId;
        }

        String result = transform("music_survey_and_scavenge", MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);
        if (MASTER_LOGGING) LOG.info("Survey music: " + result);
        return result;
    }

    @Override
    protected String getStarSystemMusicSetId()
    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet != null && playerFleet.getContainingLocation() instanceof StarSystemAPI)
        {
            StarSystemAPI sys = (StarSystemAPI) playerFleet.getContainingLocation();

            String musicSetId = sys.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY_MISSION);
            if (musicSetId != null) return musicSetId;
            musicSetId = sys.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
            if (musicSetId != null) return musicSetId;

            if (sys.hasTag(Tags.SYSTEM_ABYSSAL))
            {
                String result = transform("music_campaign_abyssal", MusicOption.of(getSetting("Abyss_Music")), PREFIX_CUSTOM);
                if (MASTER_LOGGING) LOG.info("Abyssal Star System Music: " + result);
                return result;
            }

            if (sys.hasTag(Tags.THEME_CORE) || !Misc.getMarketsInLocation(sys, Factions.PLAYER).isEmpty())
            {
                String result = transform("music_campaign", MusicOption.of(getSetting("Campaign_Music")), PREFIX_CUSTOM);
                if (MASTER_LOGGING) LOG.info("Core Star System Music: " + result);
                return result;
            }
        }
        String result = transform("music_campaign_non_core", MusicOption.of(getSetting("Campaign_Music")), PREFIX_CUSTOM);
        if (MASTER_LOGGING) LOG.info("Non Core Star System Music: " + result);
        return result;
    }

    @Override
    protected String getEncounterMusicSetId(Object param)
    {
        String fallback = super.getEncounterMusicSetId(param);
        if (!(param instanceof SectorEntityToken)) return fallback;
        SectorEntityToken token = (SectorEntityToken) param;

        String musicSetId = token.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY_MISSION);
        if (MASTER_LOGGING) LOG.info("Memory key returning: " + musicSetId);
        if (musicSetId != null) return musicSetId;
        musicSetId = token.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
        if (MASTER_LOGGING) LOG.info("Memory key returning: " + musicSetId);
        if (musicSetId != null) return musicSetId;

        String type = token.getCustomEntityType();
        if (Entities.ABYSSAL_LIGHT.equals(type) || Entities.WRECK.equals(type) || token.hasTag(Tags.GATE))
            return transform("music_encounter_neutral", MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);

        FactionAPI faction = token.getFaction();
        if (faction == null) return fallback;

        String fid = canonicalFactionId(faction.getId());
        MusicOption opt = MusicOption.of(getSetting(fid + "_Music"));
        String encounterType = determineEncounterType(token, faction);

        if (opt == MusicOption.VANILLA) return fallback;

        String result = safeGetMusic(faction, encounterType, fallback, opt);
        if (MASTER_LOGGING) LOG.info("gave encounter type: " + encounterType + " and got back " + result);
        if (result == null) return fallback;
        return result;
    }

    @Override
    protected String getMarketMusicSetId(Object param)
    {
        String fallback = super.getMarketMusicSetId(param);
        if (!(param instanceof MarketAPI)) return fallback;
        MarketAPI market = (MarketAPI) param;

        String musicSetId = market.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY_MISSION);
        if (MASTER_LOGGING) LOG.info("Memory key: " + musicSetId);
        if (musicSetId != null) return musicSetId;
        musicSetId = market.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
        if (MASTER_LOGGING) LOG.info("Memory key: " + musicSetId);
        if (musicSetId != null) return musicSetId;

        if (market.getPrimaryEntity() != null && "station_galatia_academy".equals(market.getPrimaryEntity().getId()))
        {
            if (TutorialMissionIntel.isTutorialInProgress()) return MUSIC_ENCOUNTER_MYSTERIOUS_AGGRO;

            MusicOption option = MusicOption.of(getSetting("Galatia_Academy_Music"));
            return transform(MUSIC_GALATIA_ACADEMY, option, PREFIX_CUSTOM);
        }

        if (market.getPrimaryEntity() != null && market.getPrimaryEntity().getMemoryWithoutUpdate().getBoolean("$abandonedStation"))
            return getPlanetSurveyMusicSetId(param);

        FactionAPI faction = market.getFaction();
        if (faction == null) return fallback;

        String marketType = determineMarketType(faction);
        String fid = canonicalFactionId(faction.getId());
        MusicOption opt = MusicOption.of(getSetting(fid + "_Music"));

        if (opt == MusicOption.VANILLA) return fallback;

        String result = safeGetMusic(faction, marketType, fallback, opt);
        if (MASTER_LOGGING) LOG.info("gave market type: " + marketType + " and got back " + result);
        if (result == null) return fallback;
        return result;
    }

    private String transform(String baseId, MusicOption opt, String prefix)
    {
        switch (opt)
        {
            case CUSTOM:
                return prefix + baseId;
            case BOTH:
                return RNG.nextBoolean() ? prefix + baseId : baseId;
            default:
                return baseId;
        }
    }

    private String determineEncounterType(SectorEntityToken token, FactionAPI faction)
    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        boolean hostile = false;
        boolean knows = false;

        if (token instanceof CampaignFleetAPI)
        {
            CampaignFleetAPI f = (CampaignFleetAPI) token;
            if (f.getAI() instanceof ModularFleetAIAPI)
            {
                ModularFleetAIAPI ai = (ModularFleetAIAPI) f.getAI();
                if (ai != null && playerFleet != null)
                {
                    hostile = ai.isHostileTo(playerFleet);
                }
            }
            knows = f.knowsWhoPlayerIs();
        }

        if (faction.isAtWorst(Factions.PLAYER, RepLevel.FAVORABLE) && knows && !hostile) return "encounter_friendly";
        else if ((faction.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS) && knows) || hostile)
            return "encounter_hostile";
        else return "encounter_neutral";
    }

    private String determineMarketType(FactionAPI faction)
    {
        if (faction.isAtWorst(Factions.PLAYER, RepLevel.FAVORABLE)) return "market_friendly";
        else if (faction.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS)) return "market_hostile";
        else return "market_neutral";
    }

    private String safeGetMusic(FactionAPI faction, String type, String fallback, MusicOption option)
    {
        if (faction == null)
        {
            if (MASTER_LOGGING) LOG.info("Faction is null, returning fallback: " + fallback);
            return fallback;
        }
        Map<String, String> map = faction.getMusicMap();
        if (map == null)
        {
            if (MASTER_LOGGING) LOG.info("Faction music map is null, returning fallback: " + fallback);
            return fallback;
        }

        if (MASTER_LOGGING)
            LOG.info(String.format("Faction: %s, Type: '%s', Option: %s", faction.getId(), type, option.id));

        boolean wantCustom = (option == MusicOption.CUSTOM || option == MusicOption.FACTION_SPECIFIC);
        if (option == MusicOption.BOTH || option == MusicOption.FACTION_GENERIC_SHUFFLE) wantCustom = RNG.nextBoolean();

        String key = wantCustom ? (PREFIX_CUSTOM + type) : type;
        String result = faction.getMusicMap().get(key);
        if (wantCustom && result == null)
        {
            result = DEFAULT_CUSTOM_MUSIC_MAP.get(PREFIX_CUSTOM + type);
            if (result == null) result = map.get(type);
        }
        if (result == null) result = fallback;

        return result;
    }
}