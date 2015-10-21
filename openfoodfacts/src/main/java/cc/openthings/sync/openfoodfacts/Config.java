package cc.openthings.sync.openfoodfacts;


//data fields: http://world.openfoodfacts.org/data/data-fields.txt
//example data:
//http://fr.openfoodfacts.org/api/v0/product/2165244002857.json
//http://world.openfoodfacts.org/api/v0/product/737628064502.json
//search: http://en.openfoodfacts.org/cgi/search.pl  (GET|POST)
//ex:
//<form id="search" name="search" action="http://en.openfoodfacts.org/cgi/search.pl" method="post">
//<label for="search_terms" class="i18n" data-i18n="msg_search">Search a product by name or brand:</label>
//<input type="hidden" name="action" value="process" />
//<input type="hidden" name="jqm" value="1" />
//<input type="search" id="search_terms" name="search_terms" value="" />


//form input options:
//search_terms : txt -  product by name or brand
//search_terms2 : txt - Search for words present in the product name, generic name, brands, categories, origins and labels

//Select products with specific brands, categories, labels, origins of ingredients, manufacturing places etc.
//tagtype_{n} : (where {n} is a number > 0)
//<option value="brands">brands</option>
//<option value="categories">categories</option>
//<option value="packaging">packaging</option>
//<option value="labels">labels</option>
//<option value="origins">origins of ingredients</option>
//<option value="manufacturing_places">manufacturing or processing places</option>
//<option value="emb_codes">packager codes</option>
//<option value="purchase_places">purchase places</option>
//<option value="stores">stores</option>
//<option value="countries">countries</option>
//<option value="additives">additives</option>
//<option value="allergens">allergens</option>
//<option value="traces">traces</option>
//<option value="nutrition_grades">Nutrition grades</option>
//<option value="states">states</option>

//tag_contains_{n} :
//<option value="contains">contains</option>
//<option value="does_not_contain">does not contain</option>

//tag_{n} : txt

//Ingredients
// additives : without | with | indifferent -- Additives
// ingredients_from_palm_oil : without | with | indifferent -- Ingredients from palm oil
// ingredients_that_may_be_from_palm_oil : without | with | indifferent -- Ingredients that may be from palm oil
// ingredients_from_or_that_may_be_from_palm_oil : without | with | indifferent -- Ingredients from or that may be from palm oil

//Nutriments
//nutriment_{n} :
//<option value="energy">Energy</option>
//<option value="energy-from-fat">Energy from fat</option>
//<option value="fat">Fat</option>
//<option value="saturated-fat">Saturated fat</option>
//<option value="butyric-acid">Butyric acid (4:0)</option>
//<option value="caproic-acid">Caproic acid (6:0)</option>
//<option value="caprylic-acid">Caprylic acid (8:0)</option>
//<option value="capric-acid">Capric acid (10:0)</option>
//<option value="lauric-acid">Lauric acid (12:0)</option>
//<option value="myristic-acid">Myristic acid (14:0)</option>
//<option value="palmitic-acid">Palmitic acid (16:0)</option>
//<option value="stearic-acid">Stearic acid (18:0)</option>
//<option value="arachidic-acid">Arachidic acid (20:0)</option>
//<option value="behenic-acid">Behenic acid (22:0)</option>
//<option value="lignoceric-acid">Lignoceric acid (24:0)</option>
//<option value="cerotic-acid">Cerotic acid (26:0)</option>
//<option value="montanic-acid">Montanic acid (28:0)</option>
//<option value="melissic-acid">Melissic acid (30:0)</option>
//<option value="monounsaturated-fat">Monounsaturated fat</option>
//<option value="polyunsaturated-fat">Polyunsaturated fat</option>
//<option value="omega-3-fat">Omega 3 fatty acids</option>
//<option value="alpha-linolenic-acid">Alpha-linolenic acid / ALA (18:3 n-3)</option>
//<option value="eicosapentaenoic-acid">Eicosapentaenoic acid / EPA (20:5 n-3)</option>
//<option value="docosahexaenoic-acid">Docosahexaenoic acid / DHA (22:6 n-3)</option>
//<option value="omega-6-fat">Omega 6 fatty acids</option>
//<option value="linoleic-acid">Linoleic acid / LA (18:2 n-6)</option>
//<option value="arachidonic-acid">Arachidonic acid / AA / ARA (20:4 n-6)</option>
//<option value="gamma-linolenic-acid">Gamma-linolenic acid / GLA (18:3 n-6)</option>
//<option value="dihomo-gamma-linolenic-acid">Dihomo-gamma-linolenic acid / DGLA (20:3 n-6)</option>
//<option value="omega-9-fat">Omega 9 fatty acids</option>
//<option value="oleic-acid">Oleic acid (18:1 n-9)</option>
//<option value="elaidic-acid">Elaidic acid (18:1 n-9)</option>
//<option value="gondoic-acid">Gondoic acid (20:1 n-9)</option>
//<option value="mead-acid">Mead acid (20:3 n-9)</option>
//<option value="erucic-acid">Erucic acid (22:1 n-9)</option>
//<option value="nervonic-acid">Nervonic acid (24:1 n-9)</option>
//<option value="trans-fat">Trans fat</option>
//<option value="cholesterol">Cholesterol</option>
//<option value="carbohydrates">Carbohydrate</option>
//<option value="sugars">Sugars</option>
//<option value="sucrose">Sucrose</option>
//<option value="glucose">Glucose</option>
//<option value="fructose">Fructose</option>
//<option value="lactose">Lactose</option>
//<option value="maltose">Maltose</option>
//<option value="maltodextrins">Maltodextrins</option>
//<option value="starch">Starch</option>
//<option value="polyols">Sugar alcohols (Polyols)</option>
//<option value="fiber">Dietary fiber</option>
//<option value="proteins">Proteins</option>
//<option value="casein">casein</option>
//<option value="serum-proteins">Serum proteins</option>
//<option value="nucleotides">Nucleotides</option>
//<option value="salt">Salt</option>
//<option value="sodium">Sodium</option>
//<option value="alcohol">Alcohol</option>
//<option value="vitamin-a">Vitamin A</option>
//<option value="vitamin-d">Vitamin D</option>
//<option value="vitamin-e">Vitamin E</option>
//<option value="vitamin-k">Vitamin K</option>
//<option value="vitamin-c">Vitamin C (ascorbic acid)</option>
//<option value="vitamin-b1">Vitamin B1 (Thiamin)</option>
//<option value="vitamin-b2">Vitamin B2 (Riboflavin)</option>
//<option value="vitamin-pp">Vitamin B3 / Vitamin PP (Niacin)</option>
//<option value="vitamin-b6">Vitamin B6 (Pyridoxin)</option>
//<option value="vitamin-b9">Vitamin B9 (Folic acid / Folates)</option>
//<option value="vitamin-b12">Vitamin B12 (cobalamin)</option>
//<option value="biotin">Biotin</option>
//<option value="pantothenic-acid">Pantothenic acid / Pantothenate (Vitamin B5)</option>
//<option value="silica">Silica</option>
//<option value="bicarbonate">Bicarbonate</option>
//<option value="potassium">Potassium</option>
//<option value="chloride">Chloride</option>
//<option value="calcium">Calcium</option>
//<option value="phosphorus">Phosphorus</option>
//<option value="iron">Iron</option>
//<option value="magnesium">Magnesium</option>
//<option value="zinc">Zinc</option>
//<option value="copper">Copper</option>
//<option value="manganese">Manganese</option>
//<option value="fluoride">Fluoride</option>
//<option value="selenium">Selenium</option>
//<option value="chromium">Chromium</option>
//<option value="molybdenum">Molybdenum</option>
//<option value="iodine">Iodine</option>
//<option value="caffeine">Caffeine</option>
//<option value="taurine">Taurine</option>
//<option value="ph">pH</option>
//<option value="fruits-vegetables-nuts">Fruits, vegetables and nuts (minimum)</option>
//<option value="collagen-meat-protein-ratio">Collagen/Meat protein ratio (maximum)</option>
//<option value="cocoa">Cocoa (minimum)</option>
//<option value="chlorophyl">Chlorophyl</option>
//<option value="carbon-footprint">Carbon footprint / CO2 emissions</option>
//<option value="nutrition-score-fr">Experimental nutrition score</option>
//<option value="nutrition-score-uk">Nutrition score - UK</option>

//nutriment_compare_{n} :
//<option value="lt">&lt;</option>
//<option value="lte">&lt;=</option>
//<option value="gt">&gt;</option>
//<option value="gte">&gt;=</option>
//<option value="eq">=</option>

//nutriment_value_{n} : txt

//sort_by :
//<option value="unique_scans_n">Popularity</option>
//<option value="product_name">Product name</option>
//<option value="created_t">Add date</option>
//<option value="last_modified_t">Edit date</option>

//page_size:
//<option value="20">20</option>
//<option value="50">50</option>
//<option value="100">100</option>
//<option value="250">250</option>
//<option value="500">500</option>
//<option value="1000">1000</option>

public class Config {
    private static String BASE_URL = "http://world.openfoodfacts.org/api/v0/product/";
    private static String BASE_URL_FR = "http://fr.openfoodfacts.org/api/v0/product/";

}
