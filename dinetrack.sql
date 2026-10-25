CREATE DATABASE IF NOT EXISTS dinetrack CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dinetrack;

DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS foods;
DROP TABLE IF EXISTS customers;

CREATE TABLE customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE foods (
    food_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(60) NOT NULL,
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PLACED', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PLACED',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 CHECK (total_amount >= 0),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    food_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price > 0),
    CONSTRAINT uq_order_food UNIQUE (order_id, food_id),
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_items_food FOREIGN KEY (food_id) REFERENCES foods(food_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    payment_method ENUM('CASH', 'CARD', 'UPI') NOT NULL,
    reference_code VARCHAR(120) NOT NULL,
    payment_status ENUM('SUCCESS', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'SUCCESS',
    payment_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

INSERT INTO customers (name, phone, email) VALUES
('Rahul Sharma', '9876543210', 'rahul.sharma@example.com'),
('Priya Patel', '9876543211', 'priya.patel@example.com'),
('Amit Verma', '9876543212', 'amit.verma@example.com');

INSERT INTO foods (name, category, price, available) VALUES
('Veg Burger', 'Fast Food', 120.00, TRUE),
('Margherita Pizza', 'Pizza', 250.00, TRUE),
('Paneer Tikka', 'Starters', 220.00, TRUE),
('Paneer Butter Masala', 'Main Course', 280.00, TRUE),
('Butter Naan', 'Breads', 40.00, TRUE),
('Veg Biryani', 'Main Course', 210.00, TRUE),
('French Fries', 'Sides', 90.00, TRUE),
('Cold Coffee', 'Beverages', 110.00, TRUE),
('Mango Smoothie', 'Beverages', 130.00, TRUE),
('Gulab Jamun', 'Desserts', 80.00, TRUE);

INSERT INTO orders (customer_id, status, total_amount) VALUES (1, 'COMPLETED', 360.00);
INSERT INTO order_items (order_id, food_id, quantity, unit_price) VALUES
(1, 2, 1, 250.00),
(1, 8, 1, 110.00);
INSERT INTO payments (order_id, amount, payment_method, reference_code, payment_status) VALUES
(1, 360.00, 'UPI', 'UPI-rahul@okaxis-TXN99281', 'SUCCESS');

INSERT INTO orders (customer_id, status, total_amount) VALUES (2, 'PREPARING', 360.00);
INSERT INTO order_items (order_id, food_id, quantity, unit_price) VALUES
(2, 4, 1, 280.00),
(2, 5, 2, 40.00);
