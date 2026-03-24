-- liquibase formatted sql

-- changeset eventflow:0001.1_create_roles
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) UNIQUE NOT NULL,
    name VARCHAR(64) NOT NULL
);

INSERT INTO roles (code, name) VALUES
('ADMIN', 'Administrator'),
('USER', 'User');

-- changeset eventflow:0001.2_create_users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_role_id ON users(role_id);

-- changeset eventflow:0001.3_create_email_verification_tokens
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token UUID UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- changeset eventflow:0001.4_create_events
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    creator_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    address VARCHAR(500) NOT NULL,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    poster_path VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_events_creator_id ON events(creator_id);
CREATE INDEX idx_events_starts_at ON events(starts_at);
CREATE INDEX idx_events_status ON events(status);

-- changeset eventflow:0001.5_create_event_guests
CREATE TABLE event_guests (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    registered_user_id BIGINT REFERENCES users(id),
    guest_email VARCHAR(255) NOT NULL,
    guest_token UUID UNIQUE NOT NULL,
    rsvp_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    token_active BOOLEAN NOT NULL DEFAULT TRUE,
    invited_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (event_id, guest_email)
);

CREATE INDEX idx_event_guests_registered_user_id ON event_guests(registered_user_id);
CREATE INDEX idx_event_guests_guest_token ON event_guests(guest_token);
CREATE INDEX idx_event_guests_event_rsvp ON event_guests(event_id, rsvp_status);

-- changeset eventflow:0001.6_create_polls
CREATE TABLE polls (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    created_by_user_id BIGINT NOT NULL REFERENCES users(id),
    question VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_polls_event_id ON polls(event_id);
CREATE INDEX idx_polls_event_status ON polls(event_id, status);

-- changeset eventflow:0001.7_create_poll_options
CREATE TABLE poll_options (
    id BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES polls(id),
    option_text VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (poll_id, position)
);

-- changeset eventflow:0001.8_create_poll_votes
CREATE TABLE poll_votes (
    id BIGSERIAL PRIMARY KEY,
    poll_id BIGINT NOT NULL REFERENCES polls(id),
    event_guest_id BIGINT NOT NULL REFERENCES event_guests(id),
    poll_option_id BIGINT NOT NULL REFERENCES poll_options(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (poll_id, event_guest_id)
);