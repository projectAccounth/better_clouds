-- from ClassicFastMeshBuilder

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

CloudGrid.forEachOccupied(function(cell)
    local color = Config.layer.APPEARANCE.PRESERVE_ORIGINAL_TEXTURE_COLOR and cell.color or ARGB.WHITE;
    buildCell(cell.localX, cell.localZ, color);
end)

