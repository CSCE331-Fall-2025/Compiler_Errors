--1. Show 1 week of orders from given date (replace date in query with date of interest)
SELECT COUNT(*) FROM orderhistoryce WHERE date >= DATE '2023-08-29' AND date < DATE '2023-08-29' + INTERVAL '7 days';

--2. Show profit of all orders in a given hour
SELECT SUM(price) FROM orderhistoryce WHERE EXTRACT(HOUR FROM time) = 12;

--3. Show top 2 days by profit (or by any day)
SELECT date, SUM(price) AS profit FROM orderhistoryce GROUP BY date ORDER BY profit DESC LIMIT 2;

--5. Show best selling item from worst profitable day
WITH daily_profits AS (
    SELECT date, SUM(price) AS total_profit
    FROM orderhistoryce
    GROUP BY date
),
lowest_profit_day AS (
    SELECT date
    FROM daily_profits
    ORDER BY total_profit ASC
    LIMIT 1
)
SELECT item, COUNT(*) AS occurrences
FROM orderhistoryce
WHERE date = (SELECT date FROM lowest_profit_day)
GROUP BY item
ORDER BY occurrences DESC
LIMIT 1;

