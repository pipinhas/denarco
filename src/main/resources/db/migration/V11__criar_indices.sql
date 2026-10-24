-- Índices nas colunas mais usadas em filtros, contagens e no mapa.
-- O PostgreSQL não cria índice sozinho nas chaves estrangeiras.
CREATE INDEX idx_demanda_status ON demanda (status);
CREATE INDEX idx_demanda_setor ON demanda (setor_id);
CREATE INDEX idx_demanda_categoria ON demanda (categoria_id);
CREATE INDEX idx_demanda_responsavel ON demanda (responsavel_id);
CREATE INDEX idx_demanda_autor ON demanda (autor_id);
CREATE INDEX idx_demanda_bairro ON demanda (bairro);
CREATE INDEX idx_demanda_criada_em ON demanda (criada_em);
CREATE INDEX idx_demanda_local ON demanda (latitude, longitude);
CREATE INDEX idx_historico_demanda ON historico_demanda (demanda_id);
CREATE INDEX idx_apoio_usuario ON apoio (usuario_id);
