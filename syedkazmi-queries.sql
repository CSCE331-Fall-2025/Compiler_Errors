-- 6. list of all employees with their type, email, and phone number, sorted by name
SELECT 
    name, 
    employeetype, 
    email, 
    phonenum
FROM 
    employeesce
ORDER BY 
    name;


-- 10. count number of ingredients per menu item
SELECT 
    name AS menu_item,
    array_length(string_to_array(ingredients, ','), 1) AS ingredient_count
FROM 
    menuce
ORDER BY 
    ingredient_count DESC NULLS LAST
LIMIT 20;


-- 11. dishes with more than 5 ingredients listed
SELECT  
    name AS menu_item, 
    array_length(string_to_array(ingredients, ','), 1) AS ingredient_count
FROM 
    menuce
WHERE 
    array_length(string_to_array(ingredients, ','), 1) > 5;


-- 12. employees with missing email or phone number
SELECT 
    name, 
    employeetype, 
    email, 
    phonenum
FROM 
    employeesce
WHERE 
    email IS NULL 
    OR email = ''
    OR phonenum IS NULL 
    OR phonenum = '';
