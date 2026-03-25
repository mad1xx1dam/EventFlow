import { MapContainer, Marker, TileLayer } from "react-leaflet";
import type { LatLngExpression } from "leaflet";

interface EventMapPreviewProps {
  lat: number | null;
  lon: number | null;
}

const EventMapPreview = ({ lat, lon }: EventMapPreviewProps) => {
  if (lat === null || lon === null) {
    return (
      <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-500">
        Координаты для этого мероприятия не указаны.
      </div>
    );
  }

  const position: LatLngExpression = [lat, lon];

  return (
    <div className="overflow-hidden rounded-2xl border border-slate-200">
      <MapContainer center={position} zoom={13} scrollWheelZoom={false} className="h-72 w-full" attributionControl={false}>
        <TileLayer
          attribution='&copy; OpenStreetMap contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Marker position={position} />
      </MapContainer>
    </div>
  );
};

export default EventMapPreview;