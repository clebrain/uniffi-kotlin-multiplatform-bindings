/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

use paste::paste;
use uniffi_bindgen::backend::Literal;
use uniffi_bindgen::interface::{ComponentInterface, Radix, Type};

use super::CodeType;

fn render_literal(literal: &Literal, _ci: &ComponentInterface) -> String {
    fn typed_number(type_: &Type, num_str: String) -> String {
        match type_ {
            // Bytes, Shorts and Ints can all be inferred from the type.
            Type::Int8 | Type::Int16 | Type::Int32 => num_str,
            Type::Int64 => format!("{num_str}L"),

            Type::UInt8 | Type::UInt16 | Type::UInt32 => format!("{num_str}u"),
            Type::UInt64 => format!("{num_str}uL"),

            Type::Float32 => format!("{num_str}f"),
            Type::Float64 => num_str,
            _ => panic!("Unexpected literal: {num_str} is not a number"),
        }
    }

    match literal {
        Literal::Boolean(v) => format!("{v}"),
        Literal::String(s) => format!("\"{s}\""),
        Literal::Int(i, radix, type_) => typed_number(
            type_,
            match radix {
                Radix::Octal => format!("{i:#x}"),
                Radix::Decimal => format!("{i}"),
                Radix::Hexadecimal => format!("{i:#x}"),
            },
        ),
        Literal::UInt(i, radix, type_) => typed_number(
            type_,
            match radix {
                Radix::Octal => format!("{i:#x}"),
                Radix::Decimal => format!("{i}"),
                Radix::Hexadecimal => format!("{i:#x}"),
            },
        ),
        Literal::Float(string, type_) => typed_number(type_, string.clone()),

        _ => unreachable!("Literal"),
    }
}

macro_rules! impl_code_type_for_primitive {
    ($T:ty, $class_name:literal, $canonical_name:literal) => {
        paste! {
            #[derive(Debug)]
            pub struct $T;

            impl CodeType for $T  {
                fn type_label(&self, _ci: &ComponentInterface) -> String {
                    $class_name.into()
                }

                fn canonical_name(&self) -> String {
                    $canonical_name.into()
                }

                fn literal(&self, literal: &Literal, ci: &ComponentInterface) -> String {
                    render_literal(&literal, ci)
                }
            }
        }
    };
}

impl_code_type_for_primitive!(BooleanCodeType, "kotlin.Boolean", "Boolean");
impl_code_type_for_primitive!(StringCodeType, "kotlin.String", "String");
impl_code_type_for_primitive!(BytesCodeType, "kotlin.ByteArray", "ByteArray");
impl_code_type_for_primitive!(Int8CodeType, "kotlin.Byte", "Byte");
impl_code_type_for_primitive!(Int16CodeType, "kotlin.Short", "Short");
impl_code_type_for_primitive!(Int32CodeType, "kotlin.Int", "Int");
impl_code_type_for_primitive!(Int64CodeType, "kotlin.Long", "Long");
impl_code_type_for_primitive!(UInt8CodeType, "kotlin.UByte", "UByte");
impl_code_type_for_primitive!(UInt16CodeType, "kotlin.UShort", "UShort");
impl_code_type_for_primitive!(UInt32CodeType, "kotlin.UInt", "UInt");
impl_code_type_for_primitive!(UInt64CodeType, "kotlin.ULong", "ULong");
impl_code_type_for_primitive!(Float32CodeType, "kotlin.Float", "Float");
impl_code_type_for_primitive!(Float64CodeType, "kotlin.Double", "Double");
