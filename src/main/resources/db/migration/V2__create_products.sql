CREATE TABLE products (
                          id           VARCHAR(64)  PRIMARY KEY,
                          name         VARCHAR(120) NOT NULL,
                          description  VARCHAR(500) NOT NULL,
                          price_paise  INTEGER      NOT NULL CHECK (price_paise > 0),
                          mrp_paise    INTEGER      NOT NULL CHECK (mrp_paise >= price_paise),
                          in_stock     BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO products (id, name, description, price_paise, mrp_paise, in_stock)
VALUES ('honey-1l', 'Pure Honey — 1 Litre',
        'Raw, unprocessed forest honey in a 1 litre jar.',
        45000, 50000, TRUE);
