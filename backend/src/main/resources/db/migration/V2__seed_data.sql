INSERT INTO roles (name)
VALUES ('ADMIN'), ('USER')
ON CONFLICT (name) DO NOTHING;

INSERT INTO comics (title, author, synopsis, cover_url, status)
VALUES
('Shadow Realm Chronicles', 'Jin Park', 'A warrior masters shadow power to save his kingdom.', 'https://images.unsplash.com/photo-1769874825261-ef30d63f6817?auto=format&fit=crop&w=1080&q=80', 'PUBLISHED'),
('The Last Elementalist', 'Sarah Chen', 'The final elementalist restores balance to a fractured world.', 'https://images.unsplash.com/photo-1768159904119-297a25a08c8e?auto=format&fit=crop&w=1080&q=80', 'PUBLISHED')
ON CONFLICT DO NOTHING;

INSERT INTO chapters (comic_id, chapter_number, title, premium)
VALUES
(1, 1, 'Chapter 1: The Beginning', FALSE),
(1, 2, 'Chapter 2: Into The Rift', FALSE),
(2, 1, 'Chapter 1: Awakening', FALSE)
ON CONFLICT (comic_id, chapter_number) DO NOTHING;

INSERT INTO chapter_pages (chapter_id, page_number, image_url)
VALUES
(1, 1, 'https://images.unsplash.com/photo-1697588501368-039d39ef3880?auto=format&fit=crop&w=1080&q=80'),
(1, 2, 'https://images.unsplash.com/photo-1760578360191-0908de251220?auto=format&fit=crop&w=1080&q=80'),
(2, 1, 'https://images.unsplash.com/photo-1763315371311-f59468cc2ddc?auto=format&fit=crop&w=1080&q=80'),
(3, 1, 'https://images.unsplash.com/photo-1762895158802-507fb6d7aa7e?auto=format&fit=crop&w=1080&q=80')
ON CONFLICT (chapter_id, page_number) DO NOTHING;
