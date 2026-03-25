import Input from "../common/Input";

interface LocationSearchInputProps {
  value: string;
  onChange: (value: string) => void;
  error?: string;
}

const LocationSearchInput = ({ value, onChange, error }: LocationSearchInputProps) => {
  return (
    <Input
      label="Адрес"
      name="address"
      type="text"
      placeholder="Выберите точку на карте или введите адрес вручную"
      value={value}
      onChange={(event) => onChange(event.target.value)}
      error={error}
    />
  );
};

export default LocationSearchInput;