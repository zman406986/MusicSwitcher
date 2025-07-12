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
    public static final String MUSIC_SWITCHER_IDENTIFIER = "Music_Switcher_Loaded";
    private static final Logger LOG = Global.getLogger(MusicSwitcherPlugin.class);
    public static final String MUSIC_SET_MEM_KEY = "$musicSetId";
    public static final String MUSIC_SET_MEM_KEY_MISSION = "$musicSetIdForMission";
    public static final String MUSIC_ENCOUNTER_MYSTERIOUS_AGGRO = "music_encounter_mysterious";
    public static final String MUSIC_GALATIA_ACADEMY = "music_galatia_academy";

    private static final String PREFIX_CUSTOM = "Custom_";
    private static final String PREFIX_ALL = "All_";

    private static final Random RNG = Misc.random;

    private enum MusicOption
    {
        CUSTOM("Custom"), BOTH("Both"), VANILLA("Vanilla"),
        FACTION_SPECIFIC("Faction Specific"), GENERIC_COMBAT("Generic Combat Music"),
        FACTION_GENERIC_SHUFFLE("Faction + Generic Shuffle"), ENTIRE_SAMPLE("Entire Sample Playlist");

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
        String value = settingsCache.computeIfAbsent(
                key, k -> Optional.ofNullable(LunaSettings.getString("Music_Switcher", k)).orElse("Vanilla"));
        return value;
    }

    private static final Map<String, String> FACTION_ALIAS;

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
        String canon = FACTION_ALIAS.getOrDefault(id, "other_faction");
        return canon;
    }

    @Override
    public String getMusicSetIdForCombat(CombatEngineAPI engine)
    {
        String vanilla = super.getMusicSetIdForCombat(engine);
        if (engine == null || engine.getContext() == null)
        {
            return vanilla;
        }

        CampaignFleetAPI otherFleet = engine.getContext().getOtherFleet();
        FactionAPI otherFaction = otherFleet != null ? otherFleet.getFaction() : null;

        MusicOption option = (otherFaction != null)
                ? MusicOption.of(getSetting(canonicalFactionId(otherFaction.getId()) + "_combat_Music"))
                : MusicOption.of(getSetting("default_combat_Music"));

        String result;
        switch (option)
        {
            case GENERIC_COMBAT:
                result = PREFIX_CUSTOM + "music_combat";
                break;
            case ENTIRE_SAMPLE:
                result = PREFIX_ALL + "music_combat";
                break;
            case FACTION_SPECIFIC:
                result = safeGetMusic(otherFaction, "Combat", vanilla);
                break;
            case FACTION_GENERIC_SHUFFLE:
                if (RNG.nextBoolean() || otherFaction == null)
                    result = PREFIX_CUSTOM + "music_combat";
                else
                    result = safeGetMusic(otherFaction, "Combat", PREFIX_CUSTOM + "music_combat");
                break;
            case VANILLA:
            default:
                result = vanilla;
        }
        return result;
    }

    @Override
    public String getMusicSetIdForTitle()
    {
        String res = transform("music_title", MusicOption.of(getSetting("Menu_Music")), PREFIX_CUSTOM);
        return res;
    }

    @Override
    protected String getHyperspaceMusicSetId()
    {
        String res = transform("music_campaign_hyperspace",
                MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);
        return res;
    }

    @Override
    protected String getPlanetSurveyMusicSetId(Object param)
    {
        SectorEntityToken token = null;
        if (param instanceof SectorEntityToken) {
            token = (SectorEntityToken) param;
        } else if (param instanceof MarketAPI) {
            token = ((MarketAPI)param).getPlanetEntity();
        }
        if (token != null) {
            String musicSetId = token.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY_MISSION);
            if (musicSetId != null) return musicSetId;
            musicSetId = token.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
            if (musicSetId != null) return musicSetId;
        }

        String res = transform("music_survey_and_scavenge",
                MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);
        return res;
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
                String res = transform("music_campaign_abyssal",
                        MusicOption.of(getSetting("Abyss_Music")), PREFIX_CUSTOM);
                return res;
            }

            if (sys.hasTag(Tags.THEME_CORE) ||
                    !Misc.getMarketsInLocation(sys, Factions.PLAYER).isEmpty())
            {
                String res = transform("music_campaign",
                        MusicOption.of(getSetting("Campaign_Music")), PREFIX_CUSTOM);
                return res;
            }
        }
        String res = transform("music_campaign_non_core",
                MusicOption.of(getSetting("Campaign_Music")), PREFIX_CUSTOM);
        return res;
    }

    @Override
    protected String getEncounterMusicSetId(Object param)
    {
        if (!(param instanceof SectorEntityToken)) return super.getEncounterMusicSetId(param);
        SectorEntityToken token = (SectorEntityToken) param;

        String musicSetId = token.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY_MISSION);
        if (musicSetId != null) return musicSetId;
        musicSetId = token.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
        if (musicSetId != null) return musicSetId;

        String type = token.getCustomEntityType();
        if (Entities.ABYSSAL_LIGHT.equals(type) || Entities.WRECK.equals(type) || token.hasTag(Tags.GATE))
        {
            String res = transform("music_encounter_neutral",
                    MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);
            return res;
        }

        if (Entities.CORONAL_TAP.equals(type) || Entities.DERELICT_GATEHAULER.equals(type))
        {
            String res = transform("music_encounter_mysterious",
                    MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);
            return res;
        }

        if (Entities.DEBRIS_FIELD_SHARED.equals(type) || token.hasTag(Tags.SALVAGE_MUSIC))
        {
            String res = transform("music_survey_and_scavenge",
                    MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);
            return res;
        }

        if (token.hasTag(Tags.SALVAGEABLE))
        {
            boolean hasDef = token.getMemoryWithoutUpdate().getBoolean("$hasDefenders");
            boolean mysterious = token.getMemoryWithoutUpdate().getBoolean("$limboMiningStation") ||
                    token.getMemoryWithoutUpdate().getBoolean("$limboWormholeCache");
            String res;
            if (hasDef)
            {
                res = transform(mysterious ? "music_encounter_mysterious" : "music_encounter_neutral",
                        MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);
            }
            else
            {
                res = transform("music_survey_and_scavenge",
                        MusicOption.of(getSetting("Exploration_Music")), PREFIX_CUSTOM);
            }
            return res;
        }

        FactionAPI faction = token.getFaction();
        if (faction == null) return super.getEncounterMusicSetId(param);

        String encounterType = determineEncounterType(token, faction);
        String res = resolveMusicId(faction, encounterType, param);
        return res;
    }

    @Override
    protected String getMarketMusicSetId(Object param)
    {
        if (!(param instanceof MarketAPI)) return super.getMarketMusicSetId(param);
        MarketAPI market = (MarketAPI) param;

        String musicSetId = market.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY_MISSION);
        if (musicSetId != null) return musicSetId;
        musicSetId = market.getMemoryWithoutUpdate().getString(MUSIC_SET_MEM_KEY);
        if (musicSetId != null) return musicSetId;

        if (market.getPrimaryEntity() != null &&
                "station_galatia_academy".equals(market.getPrimaryEntity().getId())) {
            if (TutorialMissionIntel.isTutorialInProgress()) {
                return MUSIC_ENCOUNTER_MYSTERIOUS_AGGRO;
            }
            MusicOption option = MusicOption.of(getSetting("Galatia_Academy_Music"));
            return transform(MUSIC_GALATIA_ACADEMY, option, PREFIX_CUSTOM);
        }

        if (market.getPrimaryEntity() != null &&
                market.getPrimaryEntity().getMemoryWithoutUpdate().getBoolean("$abandonedStation"))
        {
            String res = getPlanetSurveyMusicSetId(param);
            return res;
        }

        FactionAPI faction = market.getFaction();
        if (faction == null) return super.getMarketMusicSetId(param);

        String marketType = determineMarketType(faction);
        String res = resolveMusicId(faction, marketType, param);
        return res;
    }

    private String resolveMusicId(FactionAPI faction, String type, Object param)
    {
        String fid = canonicalFactionId(faction.getId());
        MusicOption opt = MusicOption.of(getSetting(fid + "_Music"));
        String transformed = transform(type, opt, PREFIX_CUSTOM);
        String result = safeGetMusic(faction, transformed, null);

        if (result == null) {
            if (type.startsWith("encounter_")) {
                if (faction.isAtWorst(Factions.PLAYER, RepLevel.FAVORABLE)) {
                    result = "music_default_encounter_friendly";
                } else if (faction.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS)) {
                    result = "music_default_encounter_hostile";
                } else {
                    result = "music_default_encounter_neutral";
                }
            } else if (type.startsWith("market_")) {
                if (faction.isAtWorst(Factions.PLAYER, RepLevel.FAVORABLE)) {
                    result = "music_default_market_friendly";
                } else if (faction.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS)) {
                    result = "music_default_market_hostile";
                } else {
                    result = "music_default_market_neutral";
                }
            }
        }

        return result != null ? result : super.getEncounterMusicSetId(param);
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
                if (ai != null && playerFleet != null) {
                    hostile = ai.isHostileTo(playerFleet);
                }
            }
            knows = f.knowsWhoPlayerIs();
        }

        if (faction.isAtWorst(Factions.PLAYER, RepLevel.FAVORABLE) && knows && !hostile)
            return "encounter_friendly";
        else if ((faction.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS) && knows) || hostile)
            return "encounter_hostile";
        else
            return "encounter_neutral";
    }

    private String determineMarketType(FactionAPI faction)
    {
        if (faction.isAtWorst(Factions.PLAYER, RepLevel.FAVORABLE))
            return "market_friendly";
        else if (faction.isAtBest(Factions.PLAYER, RepLevel.SUSPICIOUS))
            return "market_hostile";
        else
            return "market_neutral";
    }

    private String safeGetMusic(FactionAPI faction, String type, String fallback)
    {
        if (faction == null)
        {
            return fallback;
        }
        Map<String, String> map = faction.getMusicMap();
        if (map == null) return fallback;

        String id = map.get(type);

        if (id == null && type.startsWith(PREFIX_CUSTOM))
        {
            id = map.get(type.substring(PREFIX_CUSTOM.length()));
        }
        return id != null ? id : fallback;
    }
}