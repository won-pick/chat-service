CREATE TABLE IF NOT EXISTS chat_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    CONSTRAINT uq_chat_room UNIQUE (product_id, buyer_id, seller_id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    sent_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES chat_rooms(id)
);

CREATE TABLE IF NOT EXISTS chat_read_positions (
    room_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL,
    PRIMARY KEY (room_id, member_id),
    CONSTRAINT fk_chat_read_room FOREIGN KEY (room_id) REFERENCES chat_rooms(id)
);

CREATE TABLE IF NOT EXISTS chat_message_requests (
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    request_key VARCHAR(128) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    message_id BIGINT NOT NULL,
    PRIMARY KEY (room_id, sender_id, request_key),
    CONSTRAINT fk_chat_request_message FOREIGN KEY (message_id) REFERENCES chat_messages(id)
);
