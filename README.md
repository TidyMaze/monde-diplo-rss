# Le Monde Diplomatique RSS Feed

This service serves [Le Monde Diplomatique](https://www.monde-diplomatique.fr/) (LMD) as a RSS feed. Each item contains the full article (and not only a summary).

## Show me the stuff

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>Le Monde Diplomatique</title>
    <link>https://monde-diplo-rss.herokuapp.com</link>
    <description>Read full articles from Le Monde Diplomatique.</description>
    <item>
      <title>Les guetteurs d’inconnu</title>
      <link>https://www.monde-diplomatique.fr/2018/12/PIEILLER/59334</link>
      <description>En 1821, pour la première fois dans le monde, est fondée une Société de géographie ; et c’est dans la France de la Restauration, étouffante et pleine de désirs d’autres cieux. Une vingtaine d’années plus tard, les chercheurs d’horizons lointains vont avoir à leur disposition, pour les fixer, une toute nouvelle technique : la photographie. Explorateurs, voyageurs, ingénieurs de toutes nationalités vont alors, chacun à sa façon, dans le cadre d’expéditions le plus souvent scientifiques ou militaires, mettre en scène l’inconnu. Ils arpentent les colonies, ils avancent sur des terres peu répertoriées, ils prennent des clichés de paysages ignorés de leurs concitoyens, rendent visibles leurs habitants, mais aussi accompagnent les métamorphoses de lieux familiers saisis par la modernisation industrielle. Prises par diverses expéditions, scientifiques ou militaires pour l’essentiel, menées entre 1850 et 1914 et conservées par la Société de géographie (1), certaines de ces images ont contribué à nourrir l’imaginaire colonial ; d’autres ont su rendre l’ampleur de prouesses techniques ; d’autres, enfin, célèbrent des paysages immémoriaux. Mais, des vertiges des canyons du Colorado aux taureaux ailés de Ninive, des Indiens de l’Orénoque à la construction du canal de Panama, toutes sont saisissantes.</description>
      <source>Le Monde Diplomatique</source>
      <guid isPermaLink="false">https://www.monde-diplomatique.fr/2018/12/PIEILLER/59334</guid>
    </item>
  </channel>
</rss>
```

## How to use

```http request
GET https://monde-diplo-rss.herokuapp.com/feed?email=<email>&password=<password>
```

With `email` and `password` url-encoded.

## Questions

### Why build this, LMD already has a [RSS Feed](https://www.monde-diplomatique.fr/recents.xml) ?

Yup, but its feeds contains only shortened articles (`Hello World (...)`), even for users with full subscription.

### Do you store data ?

I only store an in-memory cache of articles. **Absolutely nothing is stored concerning your credentials** (they are known at the beginning of the HTTP call and forgotten right after).

### Can I get LMD for free ?

No, you need valid credentials to get this RSS feed. Credentials are checked every time.

### Does it work with english version ?

Not yet, only the main (french) one.
