CREATE TABLE bin_issuers
(
    bin       VARCHAR(6)  NOT NULL PRIMARY KEY,
    issuer_id VARCHAR(10) NOT NULL
);

INSERT INTO bin_issuers (bin, issuer_id)
VALUES ('400000', 'ISS001'),
       ('400001', 'ISS002'),
       ('400002', 'ISS003'),
       ('400003', 'ISS004'),
       ('400004', 'ISS005');
