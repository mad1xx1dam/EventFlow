import { useMemo, useState } from "react";
import {
  MapContainer,
  Marker,
  TileLayer,
  useMapEvents,
} from "react-leaflet";
import type { LatLngExpression } from "leaflet";

interface EventMapPickerProps {
  lat: number | null;
  lon: number | null;
  onLocationChange: (payload: { lat: number; lon: number; address: string }) => void;
}

const DEFAULT_CENTER: LatLngExpression = [55.751244, 37.618423];

interface MapClickHandlerProps {
  onPick: (lat: number, lon: number) => void;
}

const MapClickHandler = ({ onPick }: MapClickHandlerProps) => {
  useMapEvents({
    click(event) {
      onPick(event.latlng.lat, event.latlng.lng);
    },
  });

  return null;
};

const EventMapPicker = ({ lat, lon, onLocationChange }: EventMapPickerProps) => {
  const [isResolvingAddress, setIsResolvingAddress] = useState(false);

  const markerPosition = useMemo<LatLngExpression | null>(() => {
    if (lat === null || lon === null) {
      return null;
    }

    return [lat, lon];
  }, [lat, lon]);

  const mapCenter = markerPosition ?? DEFAULT_CENTER;

  const resolveAddress = async (nextLat: number, nextLon: number) => {
    setIsResolvingAddress(true);

    try {
      const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=json&lat=${nextLat}&lon=${nextLon}`
      );

      if (!response.ok) {
        throw new Error("Не удалось определить адрес");
      }

      const data: { display_name?: string } = await response.json();

      onLocationChange({
        lat: nextLat,
        lon: nextLon,
        address: data.display_name ?? "",
      });
    } catch {
      onLocationChange({
        lat: nextLat,
        lon: nextLon,
        address: "",
      });
    } finally {
      setIsResolvingAddress(false);
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-3">
        <label className="block text-sm font-medium text-slate-700">Место проведения</label>
        {isResolvingAddress ? (
          <span className="text-xs text-slate-500">Определяем адрес...</span>
        ) : null}
      </div>

      <div className="overflow-hidden rounded-2xl border border-slate-200">
        <MapContainer
          center={mapCenter}
          zoom={13}
          scrollWheelZoom
          className="h-80 w-full"
          attributionControl={false}
        >
          <TileLayer
            attribution='&copy; OpenStreetMap contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <MapClickHandler onPick={resolveAddress} />

          {markerPosition ? <Marker position={markerPosition} /> : null}
        </MapContainer>
      </div>

      <p className="text-xs text-slate-500">
        Нажмите на карту, чтобы выбрать координаты и автоматически подставить адрес.
      </p>
    </div>
  );
};

export default EventMapPicker;