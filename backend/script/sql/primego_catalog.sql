-- PrimeGo's first normalized catalog schema for MySQL 8.
-- Import this after ry_vue.sql. Merchant IDs point to RuoYi sys_user.user_id;
-- assigning merchant roles and migrating legacy accounts is a later step.

CREATE TABLE IF NOT EXISTS pg_category (
    category_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Category ID',
    category_name VARCHAR(100) NOT NULL COMMENT 'Display name',
    category_status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active, 0 hidden',
    category_created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_pg_category_name (category_name),
    KEY idx_pg_category_status (category_status, category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PrimeGo catalog categories';

CREATE TABLE IF NOT EXISTS pg_product (
    product_id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Product ID',
    merchant_user_id BIGINT DEFAULT NULL COMMENT 'RuoYi sys_user.user_id for the merchant',
    category_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    product_description TEXT,
    product_price DECIMAL(12,2) NOT NULL,
    product_stock_quantity INT NOT NULL DEFAULT 0,
    contact_whatsapp VARCHAR(40) DEFAULT NULL,
    product_status VARCHAR(20) NOT NULL DEFAULT 'OFF_SALE' COMMENT 'ON_SALE or OFF_SALE',
    product_created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    KEY idx_pg_product_category_status (category_id, product_status, product_created_at, product_id),
    KEY idx_pg_product_status_created (product_status, product_created_at, product_id),
    KEY idx_pg_product_status_stock (product_status, product_stock_quantity),
    KEY idx_pg_product_status_price (product_status, product_price),
    KEY idx_pg_product_merchant (merchant_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PrimeGo products';

CREATE TABLE IF NOT EXISTS pg_product_image (
    image_id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL COMMENT 'Public URL or path served by the configured object store',
    image_is_primary TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (image_id),
    KEY idx_pg_product_image_lookup (product_id, image_is_primary, sort_order, image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PrimeGo product images';
