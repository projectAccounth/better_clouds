function buildCell(cellX, cellZ, colorModifier)
    local x0 = cellX * MeshConstants.CELL_SIZE;
    local x1 = x0 + MeshConstants.CELL_SIZE;
    local z0 = cellZ * MeshConstants.CELL_SIZE;
    local z1 = z0 + MeshConstants.CELL_SIZE;

    -- top face
    Mesh.quad(
        x0, 0, z1,
        x1, 0, z1,
        x1, 0, z0,
        x0, 0, z0,
        colorModifier
    );

    -- bottom face
    Mesh.quad( 
        x0, 0, z0,
        x1, 0, z0,
        x1, 0, z1,
        x0, 0, z1,
        colorModifier
    );
end

local range = Coords.getCloudGridHalfSize();

for cellX = -range, range do
    for cellZ = -range, range do
        local wrappedCellX = Coords.getWrappedX(cellX);
        local wrappedCellZ = Coords.getWrappedX(cellZ);

        local cellColor = Texture.getColor(wrappedCellX, wrappedCellZ);
        local color = LayerConfig.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR and cellColor or ARGB.WHITE;

        if (not Texture.isCellEmpty(wrappedCellX, wrappedCellZ)) then
            buildCell(cellX, cellZ, color);
        end
    end
end

