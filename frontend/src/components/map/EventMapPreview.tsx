import L, { type LatLngExpression } from "leaflet";
import { MapContainer, Marker, TileLayer } from "react-leaflet";
import "leaflet/dist/leaflet.css";

import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

delete (L.Icon.Default.prototype as any)._getIconUrl;

L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

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
      <MapContainer
        center={position}
        zoom={13}
        scrollWheelZoom={false}
        className="h-72 w-full"
        attributionControl={false}
      >
        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <Marker position={position} />
      </MapContainer>
    </div>
  );
};

export default EventMapPreview;