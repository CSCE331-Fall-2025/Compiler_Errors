--7. Display all managers 
SELECT * FROM employeesce WHERE employeetype = 'manager';

--9. Selecting cost of cooking for a specific food aka unit_price/unit cost
SELECT
    name,
    unit_price
FROM
    inventoryce
WHERE
    name = 'Cabbage'; --food you wish to search for my example its cabbage


--13.  Finding item details for a specific order number
SELECT item, qty, price FROM orderhistoryce WHERE id = 1002; --id changes to whichever order number you wish for in this example its 1002

--14. Find all ingredients with zero stock (Right now we have quantity = 0 for all of them but I figured this would be useful)
SELECT name FROM inventoryce WHERE quantity = 0; 

--15. Updating stock after delivery
UPDATE inventoryce SET quantity = quantity + 150 WHERE name = 'White Rice'; 
-- change name to name of stock you wish to change and the number for the same reason in this example its to update White Rice stock to 150
