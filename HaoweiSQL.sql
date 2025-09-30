SELECT name, price, cardinality(string_to_array(ingredients, ',')) AS ingredient_count
FROM menuce;


SELECT item, qty, date, time FROM orderhistoryce ORDER BY qty DESC;