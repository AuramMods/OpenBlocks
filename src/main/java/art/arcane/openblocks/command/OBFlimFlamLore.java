package art.arcane.openblocks.command;

import java.util.Map;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

/** The original OpenBlocks name and item-history grammar. */
final class OBFlimFlamLore {

    @FunctionalInterface
    private interface Text {
        String generate(RandomSource random, Map<String, String> variables);
    }

    private static final Text HERO = createHeroGenerator();
    private static final Text LORE = createLoreGenerator();

    private OBFlimFlamLore() {}

    static String name(final RandomSource random) {
        return render(HERO, random, Map.of());
    }

    static String history(final RandomSource random, final String player, final String item) {
        return render(LORE, random, Map.of("player", player, "item", item));
    }

    private static String render(final Text text, final RandomSource random, final Map<String, String> variables) {
        return StringUtils.capitalize(text.generate(random, variables)).replaceAll("\\s+", " ");
    }

    private static Text[] parts(final Object... values) {
        final Text[] parts = new Text[values.length];
        for (int index = 0; index < values.length; index++) {
            final Object value = values[index];
            parts[index] = value instanceof Text text ? text : (random, variables) -> value.toString();
        }
        return parts;
    }

    private static Text alt(final Object... values) {
        final Text[] parts = parts(values);
        return (random, variables) -> parts[random.nextInt(parts.length)].generate(random, variables);
    }

    private static Text seq(final Object... values) {
        final Text[] parts = parts(values);
        return (random, variables) -> {
            final var sentence = new java.util.StringJoiner(" ");
            for (final Text part : parts) {
                final String text = part.generate(random, variables);
                if (!text.isEmpty()) sentence.add(text);
            }
            return sentence.toString();
        };
    }

    private static Text word(final Object... values) {
        final Text[] parts = parts(values);
        return (random, variables) -> {
            final var word = new StringBuilder();
            for (final Text part : parts) word.append(part.generate(random, variables));
            return word.toString();
        };
    }

    private static Text opt(final float probability, final Text part) {
        return (random, variables) -> random.nextFloat() < probability ? part.generate(random, variables) : "";
    }

    private static Text sub(final String key, final String fallback) {
        return (random, variables) -> variables.getOrDefault(key, fallback);
    }

    private static Text range(final int minimum, final int maximum) {
        return (random, variables) -> Integer.toString(minimum + random.nextInt(maximum - minimum));
    }

    private static Text capitalize(final Text part) {
        return (random, variables) -> WordUtils.capitalize(part.generate(random, variables));
    }

    private static Text capitalizeFully(final Text part) {
        return (random, variables) -> WordUtils.capitalizeFully(part.generate(random, variables));
    }

    private static Text createHeroGenerator() {
        Text heroesPrefix = alt("Grunnar", "Hermann", "Sven", "Grarg", "Blarf", "Hans", "Nathan", "Oglaf", "Eric", "Bob", "Banan", "Alaric");
        Text heroesPostfix = alt("ish", "ilde", "monkeybutt", "son", "shvili", "berg", "bert", "us");
        Text heroName = word(heroesPrefix, opt(0.6f, heroesPostfix));
        Text heroOptional = alt("slightly", "sometimes", "mistakenly", "somehow", "part-time");
        Text heroAdj = alt("insane", "brave", "smelly", "philosophical", "jumping", "toothless", "burning", "heroic", "shy", "narcoleptic", "manly", "girly", "non-euclidian", "euphoric", "misanthropic", "ambivalent", "fictional", "fetishist");
        Text heroClass = alt("babycrusher", "wrestler", "nitpicker", "barber", "anesthesiologist", "sharpshooter", "plumber", "insurance salesman", "clown", "empiricist", "defenestrator", "visigoth", "nipple twister");
        Text classicHeroes = capitalizeFully(seq(heroName, "the", seq(opt(0.2f, heroOptional), heroAdj, heroClass, opt(0.2f, word("(lvl. ", range(1, 11), ")")))));

        Text firstName = alt("Bill", "Juliet", "Nigel", "Steve", "Parsnip", "Cucumber", "Ludwig", "Markus", "Sven", "Clark", "Carl", "Throatwobbler", "Raymond", "Nancy", "Brian", "Brunhilda", "Richard", "Rupert");
        Text lastNameComponent = alt("Smith", "Weston", "Banana", "Drum", "Forklift", "Ampersand", "Fruitbat", "Fhtagn", "Svenson", "Stein", "Gutenabend", "Mangrove", "Bigglesworth", "Larch", "Semicolon", "Wurst", "Nixon", "Baden", "Priapus");
        Text lastName = alt(lastNameComponent, word(lastNameComponent, "-", lastNameComponent));
        Text pseudonym = alt("Duckie", "Nosepicker", "Snort", "Bomber", "Ouch", "Anvil", "Halfslab", "Radiator", "Barbie", "Biggles", "Income Tax", "Not In Face", "Tea Time", "Twerk", "Mutalisk", "Bueno", "Sixpack", "Yellow Snow");
        Text namePrefix = alt("Dr.", "Rev.", "Ms", "Mr", "Prof.", "Hon.", "Sgt.", "Cmdr.", "Sir", "Lady", "Comrade", "His Magnificence", "Her Holiness", "The Right Honourable");
        Text middleName = alt("W.", "T.", "F.");
        Text nameSuffix = alt("M.Sc", "Ph.D", "OBE", "Jr.", "Sr.", "III", "II", "Esq.");

        final Text middleStuff = seq(opt(0.1f, middleName), opt(0.6f, word("\"", pseudonym, "\"")));
        Text modernHeroes = seq(opt(0.4f, namePrefix),
                firstName,
                middleStuff,
                opt(0.3f, alt("von", "de", "van", "van de", "de la")),
                lastName,
                opt(0.2f, word(" ", nameSuffix)));

        return alt(classicHeroes, modernHeroes);
    }

    private static Text createLoreGenerator() {
        Text adj1 = alt("overpowered", "misspelled", "store-brand", "unsettling", "unremarkable", "sleazy", "boring", "golden", "junky", "ergonomic", "low voltage", "many-angled");
        Text adj2 = alt("cursed", "legendary", "unique", "penultimate", "awesome", "suboptimal", "mighty", "ridiculously", "slightly");
        Text adjs = seq(opt(0.7f, adj2), adj1);
        Text parts = alt("codpiece", "loincloth", "tootbrush", "dental floss", "eggbeater", "rubber chicken with a pulley in the middle", "shovel", "hammoc", "panties", "spatula", "fedora");

        Text placeAdj = alt("deadly", "dreadful", "boring", "cheap", "backwater", "tax-free", "gluten-free", "dark", "evil", "misunderstood");
        Text kingdomAdjective = alt("loathing", "meat", "potatoes", "hydrocarbonates", "sweden", "slighlty unpleasant things", "herpaderp", "sobbing", "knitting");
        Text kingdomish = seq(opt(0.4f, placeAdj), alt("kingdom", "cave", "gorge", "convention", "pit", "bazaar", "land"));
        Text placeWithAdj = seq(kingdomish, "of", kingdomAdjective, opt(0.2f, seq("and", kingdomAdjective)));
        Text mountainName = alt("lard", "butter", "rotten eggs", "brimstone", "newts", "doom", "croc", "flipflop");
        Text mountain = seq(opt(0.6f, placeAdj), "Mt.", mountainName);
        final Text hardcodedPlaces = alt("dalania", "prussia", "foobaria", "hot dog stand", "abyssinia", "zanzibar", "eastasia", "freedonia", "latveria", "woolloomooloo", "breslau", "uzbekistan", "north korea", "lower intestine", "hyperborea");
        Text places = capitalizeFully(alt(placeWithAdj, mountain, hardcodedPlaces));

        Text otherPeople = alt("youtube personalities", "dwarves", "villagers", "elves", "tax collectors", "quality testers", "boring people");
        Text actor = seq(alt(HERO, seq(otherPeople, "of", places)));

        Text story = alt("that nobody cares about", seq("that previously belonged to", actor));
        Text epicLoot = seq(opt(0.5f, adjs), parts, story);

        Text created = seq(alt("repurposed from", "originally bundled with ", "forged from", "not to be mistaken with"), epicLoot);
        Text loaned = seq("loaned to", actor);
        Text forgotten = seq("forgotten in", alt("post office", "loo", "deep hole", "hurry"));
        Text origin = alt(created, seq(alt("stolen", loaned, "imagined", forgotten, "found behind couch"), "by", HERO));

        Text itemModifier = alt("replica of");
        Text itemAction = alt("beating", "bleeding", "winds", "things", word(sub("item", "thing"), "ing"), "cooking", "looting", "scrubing", "backpain", "hernia");
        Text itemType = alt(sub("item", "gizmo"), alt("gizmo", "thingmajig", "doodad", "tat", "thingie"));
        Text item = capitalize(seq(opt(0.9f, adjs), itemType, opt(0.9f, seq("of", itemAction))));
        Text fullItem = seq(opt(0.1f, itemModifier), item, opt(0.05f, seq("(TM)")));

        Text taunt = alt("wimp", "noob", "git", "fool", "that scoundrel", "scumbag");
        Text playerGet = seq(alt("stolen from", "found in", "bought in", "dug out in", "smuggled from"), places);
        Text ownerInfo = seq(playerGet, "by", opt(0.3f, seq(taunt, "named")), sub("player", "Frank"));

        Text randomItems = alt("bananas", "grapes", "hairpins", "corks", "shuffling", "squash", "penguins");
        Text organizationSpeciality = alt(randomItems, seq(randomItems, "and", randomItems));

        Text university = capitalizeFully(seq("university of", alt(hardcodedPlaces, organizationSpeciality)));

        Text institutish = alt("institute", "council", "committee");
        Text institute = capitalizeFully(seq(institutish, "of", organizationSpeciality));

        Text foundationFirst = alt("lick", "pick", "poke", "prod", "smell", "ring", "steal", "hug", "kick", "fwap");
        Text foundationSecond = alt("fish", "sauce", "leopard", "pick", "smell", "mayonaise", "steal", "grave", "derp");
        Text foundation = seq(word(capitalizeFully(foundationFirst), "-a-", capitalizeFully(foundationSecond)), "foundation");

        Text organization = alt(university, foundation, institute);

        Text restoredInfo = seq("restored by", organization);
        Text recent = seq("Recently", alt(ownerInfo, restoredInfo));

        Text extra = alt("$1.99 each", "5 quids in plain wrapper", "Accept no substitues", "Made in China", "Batteries not included", "Patent pending");
        return word(fullItem, opt(0.5f, seq(",", origin)), opt(0.5f, seq(".", recent)), opt(0.2f, seq(".", extra)));
    }

}
